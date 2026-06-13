package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChecklistExecutionActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_PHOTO = 201;

    private String assetId;
    private Integer woId;

    private TextView textAssetName, textAssetDetails, textWoContext, textTemplateName, textMinReadingHint, textEvidencePhotoName;
    private TextInputEditText editReadingValue, editNotes;
    private RadioGroup groupOverallStatus;
    private RadioButton radioOk, radioWarning, radioNg;
    private ImageView imageEvidencePhotoPreview;
    private LinearLayout layoutQuestionsContainer;
    private View progressBar;
    private Button buttonSubmit, buttonPickEvidencePhoto;
    private Spinner spinnerTemplates;

    private Gson gson = new Gson();
    private JsonObject qrInfo;
    private double lastReadingValue = 0.0;
    private int templateId = 0;
    private JsonArray templateItems = new JsonArray();
    private JsonArray templatesForSubmit = new JsonArray();
    private int templateIdFromIntent = 0;

    // Photo selection tracking
    private int currentPhotoTargetId = -1; // -1 for evidence photo, or question itemId
    private Uri evidencePhotoUri = null;
    private Map<Integer, Uri> questionPhotoUris = new HashMap<>();

    // Map to hold references to inflated question views
    private Map<Integer, View> questionViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_execution);

        assetId = getIntent().getStringExtra("extra_asset_id");
        int woIdRaw = getIntent().getIntExtra("extra_wo_id", 0);
        if (woIdRaw > 0) {
            woId = woIdRaw;
        }
        templateIdFromIntent = getIntent().getIntExtra("extra_template_id", 0);

        if (TextUtils.isEmpty(assetId)) {
            Toast.makeText(this, "Không tìm thấy thông tin tài sản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadQrInfo();
    }

    private void initViews() {
        textAssetName = findViewById(R.id.textAssetName);
        textAssetDetails = findViewById(R.id.textAssetDetails);
        textWoContext = findViewById(R.id.textWoContext);
        textTemplateName = findViewById(R.id.textTemplateName);
        textMinReadingHint = findViewById(R.id.textMinReadingHint);
        textEvidencePhotoName = findViewById(R.id.textEvidencePhotoName);

        editReadingValue = findViewById(R.id.editReadingValue);
        editNotes = findViewById(R.id.editNotes);

        groupOverallStatus = findViewById(R.id.groupOverallStatus);
        radioOk = findViewById(R.id.radioOk);
        radioWarning = findViewById(R.id.radioWarning);
        radioNg = findViewById(R.id.radioNg);

        imageEvidencePhotoPreview = findViewById(R.id.imageEvidencePhotoPreview);
        layoutQuestionsContainer = findViewById(R.id.layoutQuestionsContainer);
        progressBar = findViewById(R.id.progressBar);

        buttonPickEvidencePhoto = findViewById(R.id.buttonPickEvidencePhoto);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        spinnerTemplates = findViewById(R.id.spinnerTemplates);

        buttonPickEvidencePhoto.setOnClickListener(v -> {
            currentPhotoTargetId = -1;
            pickImage();
        });

        buttonSubmit.setOnClickListener(v -> submitChecklistResult());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh minh chứng"), REQUEST_PICK_PHOTO);
    }

    private void loadQrInfo() {
        setLoading(true);
        ApiClient.getService(this).getChecklistQrInfo(assetId, woId).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    qrInfo = response.body().data;
                    renderQrInfo(qrInfo);
                } else {
                    Toast.makeText(ChecklistExecutionActivity.this, "Không tải được thông tin thiết bị/checklist", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ChecklistExecutionActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderQrInfo(JsonObject data) {
        JsonObject asset = data.getAsJsonObject("asset");
        if (asset != null) {
            textAssetName.setText(getStr(asset, "assetName"));
            textAssetDetails.setText(getStr(asset, "assetTypeName") + " • " + getStr(asset, "locationName"));
        }

        if (woId != null) {
            textWoContext.setVisibility(View.VISIBLE);
            textWoContext.setText("Gắn với Phiếu việc: WO-" + String.format("%04d", woId));
        }

        JsonObject runtimeCounter = data.getAsJsonObject("runtimeCounter");
        if (runtimeCounter != null && runtimeCounter.has("lastReadingValue")) {
            lastReadingValue = runtimeCounter.get("lastReadingValue").getAsDouble();
            textMinReadingHint.setVisibility(View.VISIBLE);
            textMinReadingHint.setText("Số giờ chạy tối thiểu: " + lastReadingValue + " giờ (bằng hoặc lớn hơn số giờ đã lưu)");
            editReadingValue.setHint("Tối thiểu " + lastReadingValue + " giờ");
        }

        JsonArray allTemplates = data.getAsJsonArray("checklistTemplates");
        templatesForSubmit = new JsonArray();
        if (allTemplates != null) {
            JsonObject woChecklist = data.getAsJsonObject("woChecklist");
            JsonArray openTemplateIds = null;
            if (woChecklist != null && woChecklist.has("openTemplateIds")) {
                openTemplateIds = woChecklist.getAsJsonArray("openTemplateIds");
            }
            
            if (woId != null && openTemplateIds != null && openTemplateIds.size() > 0) {
                for (int i = 0; i < allTemplates.size(); i++) {
                    JsonObject tpl = allTemplates.get(i).getAsJsonObject();
                    int tplId = tpl.get("templateId").getAsInt();
                    boolean isOpen = false;
                    for (int j = 0; j < openTemplateIds.size(); j++) {
                        if (openTemplateIds.get(j).getAsInt() == tplId) {
                            isOpen = true;
                            break;
                        }
                    }
                    if (isOpen) {
                        templatesForSubmit.add(tpl);
                    }
                }
            } else {
                templatesForSubmit = allTemplates;
            }
        }

        if (templatesForSubmit.size() > 1) {
            spinnerTemplates.setVisibility(View.VISIBLE);
            textTemplateName.setText("Mẫu checklist áp dụng:");
            
            List<String> templateNames = new ArrayList<>();
            for (int i = 0; i < templatesForSubmit.size(); i++) {
                templateNames.add(getStr(templatesForSubmit.get(i).getAsJsonObject(), "templateName"));
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, templateNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTemplates.setAdapter(adapter);
            
            int selectedIndex = 0;
            if (templateIdFromIntent > 0) {
                for (int i = 0; i < templatesForSubmit.size(); i++) {
                    if (templatesForSubmit.get(i).getAsJsonObject().get("templateId").getAsInt() == templateIdFromIntent) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            
            spinnerTemplates.setSelection(selectedIndex);
            
            spinnerTemplates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    JsonObject selected = templatesForSubmit.get(position).getAsJsonObject();
                    templateId = selected.get("templateId").getAsInt();
                    if (selected.has("items")) {
                        templateItems = selected.getAsJsonArray("items");
                        renderQuestions(templateItems);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            spinnerTemplates.setVisibility(View.GONE);
            JsonObject checklistTemplate = data.getAsJsonObject("checklistTemplate");
            if (checklistTemplate != null) {
                templateId = checklistTemplate.get("templateId").getAsInt();
                textTemplateName.setText("Mẫu: " + getStr(checklistTemplate, "templateName"));

                if (checklistTemplate.has("items")) {
                    templateItems = checklistTemplate.getAsJsonArray("items");
                    renderQuestions(templateItems);
                }
            } else {
                textTemplateName.setText("Không có mẫu checklist hoạt động cho loại thiết bị này");
                buttonSubmit.setEnabled(false);
            }
        }
    }

    private void renderQuestions(JsonArray items) {
        layoutQuestionsContainer.removeAllViews();
        questionViews.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            int itemId = item.get("itemId").getAsInt();
            String questionText = getStr(item, "questionText");
            String inputType = getStr(item, "inputType");

            View qView = inflater.inflate(R.layout.item_checklist_execution_question, layoutQuestionsContainer, false);
            TextView indexTv = qView.findViewById(R.id.textQuestionIndex);
            TextView textTv = qView.findViewById(R.id.textQuestionText);

            indexTv.setText("CÂU HỎI " + (i + 1));
            textTv.setText(questionText);

            RadioGroup groupPassFail = qView.findViewById(R.id.groupPassFail);
            TextInputLayout layoutEditText = qView.findViewById(R.id.layoutEditText);
            TextInputEditText editAnswer = qView.findViewById(R.id.editAnswer);
            View layoutPhotoUpload = qView.findViewById(R.id.layoutPhotoUpload);
            Button buttonPickQuestionPhoto = qView.findViewById(R.id.buttonPickQuestionPhoto);
            TextView textQuestionPhotoName = qView.findViewById(R.id.textQuestionPhotoName);
            TextView textWarningHint = qView.findViewById(R.id.textWarningHint);

            // Handle Input types
            if ("PassFail".equalsIgnoreCase(inputType)) {
                groupPassFail.setVisibility(View.VISIBLE);
                groupPassFail.setOnCheckedChangeListener((group, checkedId) -> {
                    evaluateOverallStatusSuggestion();
                });
            } else if ("Numeric".equalsIgnoreCase(inputType) || "Range".equalsIgnoreCase(inputType)) {
                layoutEditText.setVisibility(View.VISIBLE);
                editAnswer.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                layoutEditText.setHint("Nhập số trị số...");
                setupNumericWatcher(item, editAnswer, textWarningHint);
            } else if ("Text".equalsIgnoreCase(inputType)) {
                layoutEditText.setVisibility(View.VISIBLE);
                layoutEditText.setHint("Nhập mô tả...");
                editAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            } else if ("Photo".equalsIgnoreCase(inputType)) {
                layoutPhotoUpload.setVisibility(View.VISIBLE);
                buttonPickQuestionPhoto.setOnClickListener(v -> {
                    currentPhotoTargetId = itemId;
                    pickImage();
                });
            }

            layoutQuestionsContainer.addView(qView);
            questionViews.put(itemId, qView);
        }
    }

    private void setupNumericWatcher(JsonObject item, TextInputEditText editAnswer, TextView textWarningHint) {
        editAnswer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String valStr = s.toString().trim();
                if (valStr.isEmpty()) {
                    textWarningHint.setVisibility(View.GONE);
                    evaluateOverallStatusSuggestion();
                    return;
                }
                try {
                    double val = Double.parseDouble(valStr);
                    double min = item.has("safeNumericMin") && !item.get("safeNumericMin").isJsonNull() ? item.get("safeNumericMin").getAsDouble() : Double.NEGATIVE_INFINITY;
                    double max = item.has("safeNumericMax") && !item.get("safeNumericMax").isJsonNull() ? item.get("safeNumericMax").getAsDouble() : Double.POSITIVE_INFINITY;

                    if (val < min || val > max) {
                        String suggest = getStr(item, "outOfRangeSuggest");
                        if (suggest.isEmpty()) suggest = "WARNING";
                        textWarningHint.setVisibility(View.VISIBLE);
                        textWarningHint.setText("[Cảnh báo] Trị số ngoài ngưỡng an toàn! Đề xuất: " + suggest);
                    } else {
                        textWarningHint.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    textWarningHint.setVisibility(View.GONE);
                }
                evaluateOverallStatusSuggestion();
            }
        });
    }

    private void evaluateOverallStatusSuggestion() {
        String recommendedStatus = "OK";

        for (JsonElement el : templateItems) {
            JsonObject item = el.getAsJsonObject();
            int itemId = item.get("itemId").getAsInt();
            String inputType = getStr(item, "inputType");
            View qView = questionViews.get(itemId);
            if (qView == null) continue;

            if ("PassFail".equalsIgnoreCase(inputType)) {
                RadioButton radioFail = qView.findViewById(R.id.radioFail);
                if (radioFail.isChecked()) {
                    String suggest = getStr(item, "passFailFailSuggest");
                    if ("NG".equalsIgnoreCase(suggest)) {
                        recommendedStatus = "NG";
                    } else if (!"NG".equals(recommendedStatus)) {
                        recommendedStatus = "WARNING";
                    }
                }
            } else if ("Numeric".equalsIgnoreCase(inputType) || "Range".equalsIgnoreCase(inputType)) {
                TextInputEditText editAnswer = qView.findViewById(R.id.editAnswer);
                String valStr = editAnswer.getText().toString().trim();
                if (!valStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(valStr);
                        double min = item.has("safeNumericMin") && !item.get("safeNumericMin").isJsonNull() ? item.get("safeNumericMin").getAsDouble() : Double.NEGATIVE_INFINITY;
                        double max = item.has("safeNumericMax") && !item.get("safeNumericMax").isJsonNull() ? item.get("safeNumericMax").getAsDouble() : Double.POSITIVE_INFINITY;
                        if (val < min || val > max) {
                            String suggest = getStr(item, "outOfRangeSuggest");
                            if ("NG".equalsIgnoreCase(suggest)) {
                                recommendedStatus = "NG";
                            } else if (!"NG".equals(recommendedStatus)) {
                                recommendedStatus = "WARNING";
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if ("NG".equals(recommendedStatus)) {
            radioNg.setChecked(true);
        } else if ("WARNING".equals(recommendedStatus)) {
            radioWarning.setChecked(true);
        } else {
            radioOk.setChecked(true);
        }
    }

    private void submitChecklistResult() {
        if (templateId == 0) return;

        // 1. Validate hours reading
        String readingValStr = editReadingValue.getText().toString().trim();
        Double readingValue = null;
        if (!readingValStr.isEmpty()) {
            try {
                readingValue = Double.parseDouble(readingValStr);
                if (readingValue < lastReadingValue) {
                    Toast.makeText(this, "Số giờ chạy không được nhỏ hơn mốc trước đó (" + lastReadingValue + " giờ)", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Số giờ chạy không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 2. Validate evidence photo is uploaded
        if (evidencePhotoUri == null) {
            Toast.makeText(this, "Vui lòng đính kèm ảnh minh chứng chung khi nộp checklist", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Compile details and validate answers
        JsonArray details = new JsonArray();
        for (JsonElement el : templateItems) {
            JsonObject item = el.getAsJsonObject();
            int itemId = item.get("itemId").getAsInt();
            String questionText = getStr(item, "questionText");
            String inputType = getStr(item, "inputType");
            View qView = questionViews.get(itemId);
            if (qView == null) continue;

            JsonObject detailRow = new JsonObject();
            detailRow.addProperty("questionId", itemId);
            detailRow.addProperty("questionText", questionText);
            detailRow.addProperty("inputType", inputType);

            if ("PassFail".equalsIgnoreCase(inputType)) {
                RadioButton radioPass = qView.findViewById(R.id.radioPass);
                boolean isOk = radioPass.isChecked();
                detailRow.addProperty("isOk", isOk);
                detailRow.addProperty("answerValue", isOk ? "OK" : "NG");
            } else if ("Numeric".equalsIgnoreCase(inputType) || "Range".equalsIgnoreCase(inputType) || "Text".equalsIgnoreCase(inputType)) {
                TextInputEditText editAnswer = qView.findViewById(R.id.editAnswer);
                String valStr = editAnswer.getText().toString().trim();
                if (valStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập câu trả lời cho: " + questionText, Toast.LENGTH_LONG).show();
                    return;
                }
                detailRow.addProperty("answerValue", valStr);

                // calculate isOk
                boolean isOk = true;
                if ("Numeric".equalsIgnoreCase(inputType) || "Range".equalsIgnoreCase(inputType)) {
                    try {
                        double val = Double.parseDouble(valStr);
                        double min = item.has("safeNumericMin") && !item.get("safeNumericMin").isJsonNull() ? item.get("safeNumericMin").getAsDouble() : Double.NEGATIVE_INFINITY;
                        double max = item.has("safeNumericMax") && !item.get("safeNumericMax").isJsonNull() ? item.get("safeNumericMax").getAsDouble() : Double.POSITIVE_INFINITY;
                        isOk = (val >= min && val <= max);
                    } catch (Exception e) {
                        isOk = false;
                    }
                }
                detailRow.addProperty("isOk", isOk);
            } else if ("Photo".equalsIgnoreCase(inputType)) {
                Uri qPhotoUri = questionPhotoUris.get(itemId);
                if (qPhotoUri == null) {
                    Toast.makeText(this, "Vui lòng chụp/chọn ảnh minh chứng cho câu hỏi: " + questionText, Toast.LENGTH_LONG).show();
                    return;
                }
                detailRow.addProperty("isOk", true);
                detailRow.addProperty("answerValue", ""); // Will be mapped by name on backend
            }

            details.add(detailRow);
        }

        // 4. Build multipart form parts
        List<MultipartBody.Part> parts = new ArrayList<>();
        parts.add(MultipartBody.Part.createFormData("assetId", assetId));
        parts.add(MultipartBody.Part.createFormData("templateId", String.valueOf(templateId)));
        if (woId != null) {
            parts.add(MultipartBody.Part.createFormData("woId", String.valueOf(woId)));
        }

        String overallStatus = "OK";
        if (radioNg.isChecked()) overallStatus = "NG";
        else if (radioWarning.isChecked()) overallStatus = "WARNING";
        parts.add(MultipartBody.Part.createFormData("overallStatus", overallStatus));

        String notes = editNotes.getText() == null ? "" : editNotes.getText().toString().trim();
        parts.add(MultipartBody.Part.createFormData("notes", notes));

        if (readingValue != null) {
            parts.add(MultipartBody.Part.createFormData("readingValue", String.valueOf(readingValue)));
        }

        parts.add(MultipartBody.Part.createFormData("details", gson.toJson(details)));

        // Evidence photo file part
        MultipartBody.Part generalPhotoPart = createPhotoPart("photo", evidencePhotoUri);
        if (generalPhotoPart != null) {
            parts.add(generalPhotoPart);
        }

        // Specific question photos parts
        for (Map.Entry<Integer, Uri> entry : questionPhotoUris.entrySet()) {
            int qId = entry.getKey();
            Uri uri = entry.getValue();
            MultipartBody.Part qPart = createPhotoPart("item_" + qId, uri);
            if (qPart != null) {
                parts.add(qPart);
            }
        }

        setLoading(true);
        ApiClient.getService(this).submitChecklist(parts).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(ChecklistExecutionActivity.this, "Đã gửi kết quả checklist thành công", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String msg = "Gửi checklist thất bại";
                    if (response.body() != null && response.body().message != null) {
                        msg = response.body().message;
                    }
                    Toast.makeText(ChecklistExecutionActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ChecklistExecutionActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MultipartBody.Part createPhotoPart(String fieldName, Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            byte[] bytes = readAllBytes(inputStream);
            String name = getFileName(uri);
            String mime = getContentResolver().getType(uri);
            if (mime == null || mime.trim().isEmpty()) mime = "image/jpeg";
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mime));
            return MultipartBody.Part.createFormData(fieldName, name == null ? "photo.jpg" : name, requestBody);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private String getFileName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "photo.jpg";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            String fileName = getFileName(selectedUri);

            if (currentPhotoTargetId == -1) {
                // Evidence photo
                evidencePhotoUri = selectedUri;
                textEvidencePhotoName.setText(fileName);
                imageEvidencePhotoPreview.setVisibility(View.VISIBLE);
                imageEvidencePhotoPreview.setImageURI(selectedUri);
            } else {
                // Question specific photo
                questionPhotoUris.put(currentPhotoTargetId, selectedUri);
                View qView = questionViews.get(currentPhotoTargetId);
                if (qView != null) {
                    TextView textQuestionPhotoName = qView.findViewById(R.id.textQuestionPhotoName);
                    ImageView imageQuestionPhotoPreview = qView.findViewById(R.id.imageQuestionPhotoPreview);
                    textQuestionPhotoName.setText(fileName);
                    imageQuestionPhotoPreview.setVisibility(View.VISIBLE);
                    imageQuestionPhotoPreview.setImageURI(selectedUri);
                }
            }
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!loading);
    }

    private String getStr(JsonObject obj, String prop) {
        if (obj == null || !obj.has(prop) || obj.get(prop).isJsonNull()) return "";
        return obj.get(prop).getAsString();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }
}
