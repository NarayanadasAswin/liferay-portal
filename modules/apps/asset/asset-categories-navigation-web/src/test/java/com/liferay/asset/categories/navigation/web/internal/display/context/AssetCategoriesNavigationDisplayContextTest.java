/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.asset.categories.navigation.web.internal.display.context;

import com.liferay.asset.categories.navigation.web.internal.configuration.AssetCategoriesNavigationPortletInstanceConfiguration;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetVocabularyServiceUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.portlet.PortletPreferences;
import jakarta.portlet.RenderRequest;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Aswin Narayanadas
 */
public class AssetCategoriesNavigationDisplayContextTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@AfterClass
	public static void tearDownClass() {
		_assetVocabularyLocalServiceUtilMockedStatic.close();
		_assetVocabularyServiceUtilMockedStatic.close();
		_configurationProviderUtilMockedStatic.close();
		_groupLocalServiceUtilMockedStatic.close();
	}

	@Before
	public void setUp() throws Exception {
		_companyId = RandomTestUtil.randomLong();
		_scopeGroupId = RandomTestUtil.randomLong();

		_setUpThemeDisplay();
		_setUpConfigurationProviderUtil();
	}

	@Test
	public void testGetDDMTemplateAssetVocabulariesWithOrderedPreferences()
		throws Exception {

		String childGroupERC = RandomTestUtil.randomString();

		Group childGroup = _mockGroup(_scopeGroupId, childGroupERC);

		_groupLocalServiceUtilMockedStatic.when(
			() -> GroupLocalServiceUtil.fetchGroupByExternalReferenceCode(
				childGroupERC, _companyId)
		).thenReturn(
			childGroup
		);

		String parentGroupERC = RandomTestUtil.randomString();

		Group parentGroup = _mockGroup(
			RandomTestUtil.randomLong(), parentGroupERC);

		_groupLocalServiceUtilMockedStatic.when(
			() -> GroupLocalServiceUtil.fetchGroupByExternalReferenceCode(
				parentGroupERC, _companyId)
		).thenReturn(
			parentGroup
		);

		AssetVocabulary vocabulary1 = _mockVocabulary(
			RandomTestUtil.randomLong());
		String childVocERC = RandomTestUtil.randomString();

		_assetVocabularyLocalServiceUtilMockedStatic.when(
			() ->
				AssetVocabularyLocalServiceUtil.
					fetchAssetVocabularyByExternalReferenceCode(
						childVocERC, childGroup.getGroupId())
		).thenReturn(
			vocabulary1
		);

		_assetVocabularyServiceUtilMockedStatic.when(
			() -> AssetVocabularyServiceUtil.fetchVocabulary(
				vocabulary1.getVocabularyId())
		).thenReturn(
			vocabulary1
		);

		AssetVocabulary vocabulary2 = _mockVocabulary(
			RandomTestUtil.randomLong());
		String parentVocERC = RandomTestUtil.randomString();

		_assetVocabularyLocalServiceUtilMockedStatic.when(
			() ->
				AssetVocabularyLocalServiceUtil.
					fetchAssetVocabularyByExternalReferenceCode(
						parentVocERC, parentGroup.getGroupId())
		).thenReturn(
			vocabulary2
		);

		_assetVocabularyServiceUtilMockedStatic.when(
			() -> AssetVocabularyServiceUtil.fetchVocabulary(
				vocabulary2.getVocabularyId())
		).thenReturn(
			vocabulary2
		);

		PortletPreferences portletPreferences = Mockito.mock(
			PortletPreferences.class);

		Mockito.when(
			portletPreferences.getValues(
				"assetVocabularyOrderedGroupExternalReferenceCodes", null)
		).thenReturn(
			new String[] {childGroupERC, parentGroupERC}
		);

		Mockito.when(
			portletPreferences.getValues(
				"assetVocabularyOrderedVocabularyExternalReferenceCodes", null)
		).thenReturn(
			new String[] {childVocERC, parentVocERC}
		);

		AssetCategoriesNavigationDisplayContext displayContext =
			_createDisplayContext(portletPreferences);

		List<AssetVocabulary> vocabularies =
			displayContext.getDDMTemplateAssetVocabularies();

		Assert.assertEquals(
			Arrays.asList(vocabulary1, vocabulary2), vocabularies);
	}

	private AssetCategoriesNavigationDisplayContext _createDisplayContext(
			PortletPreferences portletPreferences)
		throws Exception {

		RenderRequest renderRequest = Mockito.mock(RenderRequest.class);

		Mockito.when(
			renderRequest.getPreferences()
		).thenReturn(
			portletPreferences
		);

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();

		mockHttpServletRequest.setAttribute(
			WebKeys.THEME_DISPLAY, _themeDisplay);

		return new AssetCategoriesNavigationDisplayContext(
			mockHttpServletRequest, renderRequest);
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

	private AssetVocabulary _mockVocabulary(long vocabularyId) {
		AssetVocabulary assetVocabulary = Mockito.mock(AssetVocabulary.class);

		Mockito.when(
			assetVocabulary.getVocabularyId()
		).thenReturn(
			vocabularyId
		);

		return assetVocabulary;
	}

	private void _setUpConfigurationProviderUtil() throws Exception {
		AssetCategoriesNavigationPortletInstanceConfiguration
			assetCategoriesNavigationPortletInstanceConfiguration =
				Mockito.mock(
					AssetCategoriesNavigationPortletInstanceConfiguration.
						class);

		Mockito.when(
			assetCategoriesNavigationPortletInstanceConfiguration.
				allAssetVocabularies()
		).thenReturn(
			false
		);

		_configurationProviderUtilMockedStatic.when(
			() -> ConfigurationProviderUtil.getPortletInstanceConfiguration(
				AssetCategoriesNavigationPortletInstanceConfiguration.class,
				_themeDisplay)
		).thenReturn(
			assetCategoriesNavigationPortletInstanceConfiguration
		);
	}

	private void _setUpThemeDisplay() {
		_themeDisplay = Mockito.mock(ThemeDisplay.class);

		Mockito.when(
			_themeDisplay.getCompanyId()
		).thenReturn(
			_companyId
		);

		Mockito.when(
			_themeDisplay.getScopeGroupId()
		).thenReturn(
			_scopeGroupId
		);
	}

	private static final MockedStatic<AssetVocabularyLocalServiceUtil>
		_assetVocabularyLocalServiceUtilMockedStatic = Mockito.mockStatic(
			AssetVocabularyLocalServiceUtil.class);
	private static final MockedStatic<AssetVocabularyServiceUtil>
		_assetVocabularyServiceUtilMockedStatic = Mockito.mockStatic(
			AssetVocabularyServiceUtil.class);
	private static final MockedStatic<ConfigurationProviderUtil>
		_configurationProviderUtilMockedStatic = Mockito.mockStatic(
			ConfigurationProviderUtil.class);
	private static final MockedStatic<GroupLocalServiceUtil>
		_groupLocalServiceUtilMockedStatic = Mockito.mockStatic(
			GroupLocalServiceUtil.class);

	private long _companyId;
	private long _scopeGroupId;
	private ThemeDisplay _themeDisplay;

}