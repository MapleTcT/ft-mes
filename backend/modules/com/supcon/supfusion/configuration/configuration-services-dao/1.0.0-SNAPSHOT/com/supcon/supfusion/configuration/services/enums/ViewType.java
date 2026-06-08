package com.supcon.supfusion.configuration.services.enums;

public enum ViewType {
	EDIT, VIEW, LIST, DIGEST,/*SEARCH, ADVSEARCH,*/ MNECODE, REFERENCE, TREE, REFTREE, EXTRA;

	public static ViewType getViewType(String type) {
		if ("EDIT".equals(type)) {
			return ViewType.EDIT;
		} else if ("VIEW".equals(type)) {
			return ViewType.VIEW;
		} else if ("LIST".equals(type)) {
			return ViewType.LIST;
		} else if ("DIGEST".equals(type)) {
			return ViewType.DIGEST;
		} else if ("MNECODE".equals(type)) {
			return ViewType.MNECODE;
		} else if ("REFERENCE".equals(type)) {
			return ViewType.REFERENCE;
		} else if ("TREE".equals(type)) {
			return ViewType.TREE;
		} else if ("REFTREE".equals(type)) {
			return ViewType.REFTREE;
		} else if ("EXTRA".equals(type)) {
			return ViewType.EXTRA;
		}
		return null;
	}
}
