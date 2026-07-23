/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.asset.categories.navigation.web.internal.portlet.action;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.test.portlet.MockActionRequest;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.portlet.PortletPreferences;

import java.lang.reflect.Field;

import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Aswin Narayanadas
 */
public class AssetCategoriesNavigationConfigurationActionTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@AfterClass
	public static void tearDownClass() {
		_groupLocalServiceUtilMockedStatic.close();
	}

	@Before
	public void setUp() throws Exception {
		_childSiteGroupId = RandomTestUtil.randomLong();
		_companyId = RandomTestUtil.randomLong();

		_setUpGroupLocalServiceUtil();
	}

	@Test
	public void testPostProcessWithMixedSiteVocabularies() throws Exception {
		AssetVocabularyService assetVocabularyService = Mockito.mock(
			AssetVocabularyService.class);

		AssetVocabulary childVocabulary = _mockVocabulary(
			RandomTestUtil.randomLong(), _childSiteGroupId,
			RandomTestUtil.randomString());
		long childVocabularyId = RandomTestUtil.randomLong();

		Mockito.when(
			assetVocabularyService.fetchVocabulary(childVocabularyId)
		).thenReturn(
			childVocabulary
		);

		AssetVocabulary parentVocabulary = _mockVocabulary(
			RandomTestUtil.randomLong(), RandomTestUtil.randomLong(),
			RandomTestUtil.randomString());
		long parentVocabularyId = RandomTestUtil.randomLong();

		Mockito.when(
			assetVocabularyService.fetchVocabulary(parentVocabularyId)
		).thenReturn(
			parentVocabulary
		);

		GroupLocalService groupLocalService = Mockito.mock(
			GroupLocalService.class);

		String childGroupERC = RandomTestUtil.randomString();

		Group childGroup = _mockGroup(_childSiteGroupId, childGroupERC);

		Mockito.when(
			groupLocalService.getGroup(_childSiteGroupId)
		).thenReturn(
			childGroup
		);

		long parentGroupId = parentVocabulary.getGroupId();
		String parentGroupERC = RandomTestUtil.randomString();

		Group parentGroup = _mockGroup(parentGroupId, parentGroupERC);

		Mockito.when(
			groupLocalService.getGroup(parentGroupId)
		).thenReturn(
			parentGroup
		);

		AssetCategoriesNavigationConfigurationAction configurationAction =
			_createConfigurationAction(
				assetVocabularyService, groupLocalService);

		PortletPreferences portletPreferences = Mockito.mock(
			PortletPreferences.class);

		Mockito.when(
			portletPreferences.getValue("allAssetVocabularies", null)
		).thenReturn(
			"false"
		);

		Mockito.when(
			portletPreferences.getValue("assetVocabularyIds", null)
		).thenReturn(
			StringBundler.concat(childVocabularyId, ",", parentVocabularyId)
		);

		Mockito.when(
			portletPreferences.getMap()
		).thenReturn(
			new HashMap<>()
		);

		configurationAction.postProcess(
			_companyId, _buildMockActionRequest(), portletPreferences);

		Mockito.verify(
			portletPreferences
		).setValues(
			"assetVocabularyOrderedGroupExternalReferenceCodes", childGroupERC,
			parentGroupERC
		);

		Mockito.verify(
			portletPreferences
		).setValues(
			"assetVocabularyOrderedVocabularyExternalReferenceCodes",
			childVocabulary.getExternalReferenceCode(),
			parentVocabulary.getExternalReferenceCode()
		);
	}

	private MockActionRequest _buildMockActionRequest() {
		MockActionRequest mockActionRequest = new MockActionRequest();

		ThemeDisplay themeDisplay = Mockito.mock(ThemeDisplay.class);

		Mockito.when(
			themeDisplay.getCompanyId()
		).thenReturn(
			_companyId
		);

		Mockito.when(
			themeDisplay.getScopeGroupId()
		).thenReturn(
			_childSiteGroupId
		);

		mockActionRequest.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);

		return mockActionRequest;
	}

	private AssetCategoriesNavigationConfigurationAction
			_createConfigurationAction(
				AssetVocabularyService assetVocabularyService,
				GroupLocalService groupLocalService)
		throws Exception {

		AssetCategoriesNavigationConfigurationAction configurationAction =
			new AssetCategoriesNavigationConfigurationAction();

		_setField(
			configurationAction, "_assetVocabularyService",
			assetVocabularyService);
		_setField(configurationAction, "_groupLocalService", groupLocalService);

		return configurationAction;
	}

	private Group _mockGroup(long groupId, String externalReferenceCode) {
		Group group = Mockito.mock(Group.class);

		Mockito.when(
			group.getExternalReferenceCode()
		).thenReturn(
			externalReferenceCode
		);

		Mockito.when(
			group.getGroupId()
		).thenReturn(
			groupId
		);

		return group;
	}

	private AssetVocabulary _mockVocabulary(
		long vocabularyId, long groupId, String externalReferenceCode) {

		AssetVocabulary assetVocabulary = Mockito.mock(AssetVocabulary.class);

		Mockito.when(
			assetVocabulary.getExternalReferenceCode()
		).thenReturn(
			externalReferenceCode
		);

		Mockito.when(
			assetVocabulary.getGroupId()
		).thenReturn(
			groupId
		);

		Mockito.when(
			assetVocabulary.getVocabularyId()
		).thenReturn(
			vocabularyId
		);

		return assetVocabulary;
	}

	private void _setField(Object target, String fieldName, Object value)
		throws Exception {

		Field field = target.getClass(
		).getDeclaredField(
			fieldName
		);

		field.setAccessible(true);
		field.set(target, value);
	}

	private void _setUpGroupLocalServiceUtil() {
		_groupLocalServiceUtilMockedStatic.when(
			() -> GroupLocalServiceUtil.fetchGroup(
				Mockito.anyLong(), Mockito.any())
		).thenReturn(
			null
		);
	}

	private static final MockedStatic<GroupLocalServiceUtil>
		_groupLocalServiceUtilMockedStatic = Mockito.mockStatic(
			GroupLocalServiceUtil.class);

	private long _childSiteGroupId;
	private long _companyId;

}