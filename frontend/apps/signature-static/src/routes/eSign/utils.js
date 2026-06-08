export const toFormData = (params) => {
  const formData = new FormData();
  for (const key in params) {
    if (params) {
      formData.append(key, params[key]);
    }
  }
  return formData;
};
