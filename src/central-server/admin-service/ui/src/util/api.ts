/*
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
// Ok to ignore any here because that is the axios contract as well
/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/explicit-module-boundary-types */

import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';

/*
 * Wrapper that is used to encode path variables in URL-s
 */
export function encodePathParameter(value: string | number | boolean): string {
  return encodeURIComponent(value);
}

export type PostPutPatch = <T>(
  uri: string,
  data: any,
  config?: AxiosRequestConfig,
) => Promise<AxiosResponse<T>>;

/*
 * Wraps axios and post method calls with data
 */
export function post<T>(
  uri: string,
  data: any,
  config?: AxiosRequestConfig,
): Promise<AxiosResponse<T>> {
  return axios.post<T>(uri, data, config);
}

/*
 * Wraps axios patch method calls with data
 */
export function patch<T>(
  uri: string,
  data: any,
  config?: AxiosRequestConfig,
): Promise<AxiosResponse<T>> {
  return axios.patch<T>(uri, data, config);
}

/*
 * Wraps axios put method calls with data
 */
export function put<T>(
  uri: string,
  data: any,
  config?: AxiosRequestConfig,
): Promise<AxiosResponse<T>> {
  return axios.put<T>(uri, data, config);
}

/*
 * Wraps axios delete method
 */
export function remove<T>(uri: string): Promise<AxiosResponse<T>> {
  return axios.delete<T>(uri);
}

/*
 * Wraps axios get method calls
 */
export function get<T>(
  uri: string,
  config?: AxiosRequestConfig,
): Promise<AxiosResponse<T>> {
  return axios.get<T>(uri, config);
}
