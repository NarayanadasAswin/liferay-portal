/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getItems} from '../../../src/main/resources/META-INF/resources/js/utils/client.es';

const ENDPOINT = '/o/data-engine/v2.0/test-api';

function getPageJSON({
	items = [],
	lastPage = 1,
	page = 1,
	pageSize = 250,
	totalCount = items.length,
}) {
	return JSON.stringify({items, lastPage, page, pageSize, totalCount});
}

describe('getItems', () => {
	beforeEach(() => {
		jest.clearAllMocks();
	});

	it('returns the items when they fit on a single page', async () => {
		const items = [{id: 1}, {id: 2}];

		fetch.mockResponseOnce(getPageJSON({items}));

		expect(await getItems(ENDPOINT)).toEqual(items);
		expect(fetch).toHaveBeenCalledTimes(1);
	});

	it('sends the keywords it is given', async () => {
		fetch.mockResponseOnce(getPageJSON({}));

		await getItems(ENDPOINT, 'a b&c');

		const url = new URL(fetch.mock.calls[0][0]);

		expect(url.searchParams.get('keywords')).toBe('a b&c');
	});

	it('defaults keywords to an empty string when the argument is omitted', async () => {
		fetch.mockResponseOnce(getPageJSON({}));

		await getItems(ENDPOINT);

		const url = new URL(fetch.mock.calls[0][0]);

		expect(url.searchParams.get('keywords')).toBe('');
	});

	it('pages by the server page count when it clamps the requested page size', async () => {
		fetch
			.mockResponseOnce(
				getPageJSON({
					items: [{id: 1}, {id: 2}],
					lastPage: 2,
					pageSize: 2,
					totalCount: 4,
				})
			)
			.mockResponseOnce(
				getPageJSON({
					items: [{id: 3}, {id: 4}],
					lastPage: 2,
					page: 2,
					pageSize: 2,
					totalCount: 4,
				})
			);

		// totalCount / pageSize would stop after page 1

		expect(await getItems(ENDPOINT)).toEqual([
			{id: 1},
			{id: 2},
			{id: 3},
			{id: 4},
		]);
		expect(fetch).toHaveBeenCalledTimes(2);

		const [[firstURL], [secondURL]] = fetch.mock.calls;

		expect(firstURL).toContain('page=1');
		expect(secondURL).toContain('page=2');
	});

	it('rejects instead of resolving with an empty list when a request fails', async () => {
		fetch.mockResponseOnce(JSON.stringify({title: 'Forbidden'}), {
			status: 403,
		});

		await expect(getItems(ENDPOINT)).rejects.toEqual({title: 'Forbidden'});
	});

	it('threads the AbortSignal through to every request', async () => {
		fetch
			.mockResponseOnce(
				getPageJSON({
					items: [{id: 1}],
					lastPage: 2,
					pageSize: 1,
					totalCount: 2,
				})
			)
			.mockResponseOnce(
				getPageJSON({
					items: [{id: 2}],
					lastPage: 2,
					page: 2,
					pageSize: 1,
					totalCount: 2,
				})
			);

		const abortController = new AbortController();

		await getItems(ENDPOINT, '', {
			pageSize: 1,
			signal: abortController.signal,
		});

		expect(fetch).toHaveBeenCalledTimes(2);

		fetch.mock.calls.forEach(([, options]) => {
			expect(options.signal).toBe(abortController.signal);
		});
	});
});
