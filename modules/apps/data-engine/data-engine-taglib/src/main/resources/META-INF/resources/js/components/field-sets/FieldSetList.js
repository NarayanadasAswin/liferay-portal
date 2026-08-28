/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayEmptyState from '@clayui/empty-state';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useDebounce} from '@clayui/shared';
import {
	DRAG_TYPES,
	EVENT_TYPES,
	useConfig,
	useForm,
	useFormState,
} from 'data-engine-js-components-web';
import React, {useEffect, useState} from 'react';

import {getItems} from '../../utils/client.es';
import {getLocalizedValue, getPluralMessage, sub} from '../../utils/lang.es';
import {getSearchRegex} from '../../utils/search.es';
import {errorToast} from '../../utils/toast.es';
import FieldType from '../field-types/FieldType.es';
import FieldSetModal from './FieldSetModal';
import useDeleteFieldSet from './actions/useDeleteFieldSet.es';
import usePropagateFieldSet from './actions/usePropagateFieldSet.es';

function matchesSearchTerm(fieldSet, searchTerm) {
	try {
		const regex = getSearchRegex(searchTerm);

		return regex.test(
			getLocalizedValue(fieldSet.defaultLanguageId, fieldSet.name)
		);
	}
	catch (error) {
		return false;
	}
}

function getSortedFieldsets(fieldsets) {
	return [...fieldsets].sort((a, b) => {
		const localizedValueA = getLocalizedValue(a.defaultLanguageId, a.name);
		const localizedValueB = getLocalizedValue(b.defaultLanguageId, b.name);

		return localizedValueA.localeCompare(localizedValueB);
	});
}

export default function FieldSetList({searchTerm}) {
	const [modalState, setModalState] = useState({isVisible: false});
	const {fieldSets} = useFormState();
	const {dataDefinition} = useFormState({schema: ['dataDefinition']});
	const dispatch = useForm();
	const deleteFieldSet = useDeleteFieldSet();
	const propagateFieldSet = usePropagateFieldSet();

	const {contentType, dataDefinitionId, groupId} = useConfig();

	// searchState always carries the term its items belong to, so the list is
	// never rendered under a term it was not searched with

	const [searchState, setSearchState] = useState(null);

	// A blank term is null to the API, which would fall back to fetching every
	// fieldset, so only a trimmed term is searched

	const trimmedSearchTerm = searchTerm ? searchTerm.trim() : '';

	const debouncedSearchTerm = useDebounce(trimmedSearchTerm, 300);

	useEffect(() => {
		if (!contentType || !debouncedSearchTerm) {
			setSearchState(null);

			return;
		}

		const abortController = new AbortController();

		const {signal} = abortController;

		const searchFieldSets = async () => {
			try {
				const siteFieldSetsPromise = groupId
					? getItems(
							`/o/data-engine/v2.0/sites/${groupId}/data-definitions/by-content-type/${contentType}`,
							debouncedSearchTerm,
							{signal}
						)
					: Promise.resolve([]);

				const globalFieldSetsPromise =
					groupId === themeDisplay.getCompanyGroupId()
						? Promise.resolve([])
						: getItems(
								`/o/data-engine/v2.0/data-definitions/by-content-type/${contentType}`,
								debouncedSearchTerm,
								{signal}
							);

				const [siteFieldSets, globalFieldSets] = await Promise.all([
					siteFieldSetsPromise,
					globalFieldSetsPromise,
				]);

				if (!signal.aborted) {
					setSearchState({
						items: [...siteFieldSets, ...globalFieldSets].filter(
							({id}) => id !== parseInt(dataDefinitionId, 10)
						),
						term: debouncedSearchTerm,
					});
				}
			}
			catch (error) {
				if (!signal.aborted) {

					// A failed search must not read as an empty one, or an
					// indexing problem looks like a missing fieldset

					setSearchState({
						hasError: true,
						items: [],
						term: debouncedSearchTerm,
					});

					errorToast();
				}
			}
		};

		searchFieldSets();

		return () => abortController.abort();
	}, [contentType, dataDefinitionId, debouncedSearchTerm, groupId]);

	// Saving a fieldset rebuilds fieldSets, which the search results are
	// independent of, so an edited row has to be refreshed from there and a
	// created one added, as long as it matches the term it would appear under

	useEffect(() => {
		setSearchState((searchState) => {
			if (!searchState) {
				return searchState;
			}

			const items = searchState.items.map(
				(item) => fieldSets.find(({id}) => id === item.id) ?? item
			);

			const ids = new Set(items.map(({id}) => id));

			return {
				...searchState,
				items: [
					...items,
					...fieldSets.filter(
						(fieldSet) =>
							!ids.has(fieldSet.id) &&
							fieldSet.id !== parseInt(dataDefinitionId, 10) &&
							matchesSearchTerm(fieldSet, searchState.term)
					),
				],
			};
		});
	}, [dataDefinitionId, fieldSets]);

	// True for the whole debounce and request window, not only the first search

	const isSearching =
		!!trimmedSearchTerm && searchState?.term !== trimmedSearchTerm;

	const displayedFieldsets = getSortedFieldsets(
		trimmedSearchTerm ? searchState?.items ?? [] : fieldSets
	);

	const fieldSetsInUse = new Set();
	dataDefinition.dataDefinitionFields.forEach(
		({customProperties: {ddmStructureId}, fieldType}) => {
			if (fieldType === 'fieldset') {
				fieldSetsInUse.add(parseInt(ddmStructureId, 10));
			}
		}
	);

	const toggleFieldSet = (fieldSet) => {
		setModalState(({isVisible}) => ({
			fieldSet,
			isVisible: !isVisible,
		}));
	};

	// Deleting only rebuilds fieldSets, which the search results are
	// independent of, so the row has to be dropped from them here

	const deleteSearchedFieldSet = async (fieldSet) => {
		const isDeleted = await deleteFieldSet(fieldSet);

		if (isDeleted) {
			setSearchState(
				(searchState) =>
					searchState && {
						...searchState,
						items: searchState.items.filter(
							({id}) => id !== fieldSet.id
						),
					}
			);
		}

		return isDeleted;
	};

	const CreateNewFieldsetButton = () => (
		<ClayButton
			block
			className="add-fieldset"
			displayType="secondary"
			onClick={() => toggleFieldSet()}
		>
			{Liferay.Language.get('create-new-fieldset')}
		</ClayButton>
	);

	return (
		<>
			{!!displayedFieldsets.length || isSearching ? (
				<>
					<CreateNewFieldsetButton />

					<div className="mt-3">
						{isSearching ? (
							<ClayLoadingIndicator />
						) : (
							displayedFieldsets.map((fieldSet) => {
								const actions = [
									{
										action: () => toggleFieldSet(fieldSet),
										name: Liferay.Language.get('edit'),
									},
									{
										action: () =>
											propagateFieldSet({
												fieldSet,
												isDeleteAction: true,
												modal: {
													actionMessage:
														Liferay.Language.get(
															'delete'
														),
													fieldSetMessage:
														Liferay.Language.get(
															'the-fieldset-will-be-deleted-permanently-from'
														),
													headerMessage:
														Liferay.Language.get(
															'delete'
														),
													status: 'danger',
													warningMessage:
														Liferay.Language.get(
															'this-action-may-erase-data-permanently'
														),
												},
												onPropagate:
													deleteSearchedFieldSet,
											}),
										name: Liferay.Language.get('delete'),
									},
								];
								const description = getPluralMessage(
									Liferay.Language.get('x-field'),
									Liferay.Language.get('x-fields'),
									fieldSet.dataDefinitionFields.length
								);
								const disabled = fieldSetsInUse.has(
									fieldSet.id
								);
								const label = getLocalizedValue(
									fieldSet.defaultLanguageId,
									fieldSet.name
								);
								const onDoubleClick = () => {
									dispatch({
										payload: {fieldSet},
										type: EVENT_TYPES.FIELD_SET.ADD,
									});
								};

								return (
									<FieldType
										actions={actions}
										description={description}
										disabled={disabled}
										dragType={DRAG_TYPES.DRAG_FIELDSET_ADD}
										fieldSet={fieldSet}
										icon="forms"
										key={fieldSet.dataDefinitionKey}
										label={label}
										onDoubleClick={onDoubleClick}
									/>
								);
							})
						)}
					</div>
				</>
			) : (
				<div className="mt-2">
					{searchState?.hasError ? (
						<ClayEmptyState
							description={Liferay.Language.get(
								'an-unexpected-error-occurred'
							)}
							imgSrc={`${themeDisplay.getPathThemeImages()}/states/search_state.svg`}
							small
							title={Liferay.Language.get(
								'unable-to-load-content'
							)}
						/>
					) : trimmedSearchTerm ? (
						<ClayEmptyState
							description={sub(
								Liferay.Language.get(
									'there-are-no-results-for-x'
								),
								[trimmedSearchTerm]
							)}
							imgSrc={`${themeDisplay.getPathThemeImages()}/states/search_state.svg`}
							small
							title={Liferay.Language.get('no-results-found')}
						/>
					) : (
						<ClayEmptyState
							description={Liferay.Language.get(
								'there-are-no-fieldsets-description'
							)}
							imgSrc={`${themeDisplay.getPathThemeImages()}/states/empty_state.svg`}
							small
							title={Liferay.Language.get(
								'there-are-no-fieldsets'
							)}
						>
							<CreateNewFieldsetButton />
						</ClayEmptyState>
					)}
				</div>
			)}
			{modalState.isVisible && (
				<FieldSetModal
					fieldSet={modalState.fieldSet}
					onClose={toggleFieldSet}
				/>
			)}
		</>
	);
}
