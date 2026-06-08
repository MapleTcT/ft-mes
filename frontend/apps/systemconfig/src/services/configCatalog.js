import { request } from 'sup-rc-utility';

export function fetchConfigCatalogs(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  delete params.keyword;
  return request({
    url: `/inter-api/systemconfig/v1/config/catalog?keyword=${keyword}`,
    method: 'GET',
    params
  });
}

export function fetchCatalogInfo(catalogId) {
  return request({
    url: `/inter-api/systemconfig/v1/config/catalog/${catalogId}`,
    method: 'GET'
  });
}

export function updateConfigValue(data) {
  return request({
    url: '/inter-api/systemconfig/v1/config/catalog/value',
    method: 'PUT',
    data
  });
}

export function getSearchCatalogUrl() {
  return '/inter-api/systemconfig/v1/config/catalog';
}

export function fetchCatalogParent(catalogId) {
  return request({
    url: `/inter-api/systemconfig/v1/config/catalog/parent/${catalogId}`,
    method: 'GET'
  });
}

export function getAuthority(code) {
  return request.get(
    `/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`
  );
}
