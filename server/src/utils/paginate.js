/**
 * paginate.js — Utility phân trang chuẩn.
 * Dùng trong: services có danh sách (employees, assets...).
 */
export function getPagination(query) {
  const limit = Math.min(100, Math.max(1, parseInt(query.limit) || 20));
  let page = 1;
  let offset = 0;

  if (query.page !== undefined && query.page !== "") {
    page = Math.max(1, parseInt(query.page) || 1);
    offset = (page - 1) * limit;
  } else if (query.offset !== undefined && query.offset !== "") {
    offset = Math.max(0, parseInt(query.offset) || 0);
    page = Math.floor(offset / limit) + 1;
  } else {
    page = Math.max(1, parseInt(query.page) || 1);
    offset = (page - 1) * limit;
  }

  return { page, limit, offset };
}

export function paginatedResult(items, total, page, limit) {
  return {
    items,
    total,
    page,
    limit,
    totalPages: Math.ceil(total / limit),
  };
}
