/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

export const allDatabases = state => state.databases;
export const isAuthorised = state => (!state.isAuthenticated || state.credentials);
export const landingPage = state => state.landingPage;
export const userLogged = state => state.userLogged;
export const credentials = state => state.credentials;
export const isTypeDBRunning = state => state.isTypeDBRunning;
export const activeTab = state => state.activeTab;
