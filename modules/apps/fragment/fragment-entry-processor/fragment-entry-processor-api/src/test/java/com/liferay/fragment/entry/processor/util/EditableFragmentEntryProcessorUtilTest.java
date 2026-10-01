/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.entry.processor.util;

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Aswin Narayanadas
 */
public class EditableFragmentEntryProcessorUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetEditableTypes() throws Exception {
		_testGetEditableTypesWithEditableIdAttribute();
		_testGetEditableTypesWithEditableTag();
	}

	private void _assertEditableTypesOrder(String html) {
		Map<String, String> editableTypes =
			EditableFragmentEntryProcessorUtil.getEditableTypes(html);

		Assert.assertEquals(
			Arrays.asList(
				"title", "image", "subtitle", "description", "link", "button"),
			new ArrayList<>(editableTypes.keySet()));
	}

	private String _readFile(String fileName) throws Exception {
		Class<?> clazz = getClass();

		return StringUtil.read(
			clazz.getClassLoader(),
			"com/liferay/fragment/entry/processor/util/dependencies/" +
				fileName);
	}

	private void _testGetEditableTypesWithEditableIdAttribute()
		throws Exception {

		_assertEditableTypesOrder(_readFile("editable_id_attribute.html"));
	}

	private void _testGetEditableTypesWithEditableTag() throws Exception {
		_assertEditableTypesOrder(_readFile("editable_tag.html"));
	}

}
