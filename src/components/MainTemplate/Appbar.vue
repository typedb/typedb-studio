<!--
 Copyright (C) 2021 Grakn Labs

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

<template>
<div>
    <nav role="navigation" class="navbar-fixed noselect">
        <div class="nav-wrapper">
            <span>Grakn Workbase - Visualiser</span>
        </div>
    </nav>
</div>
</template>

<style scoped>

.nav-wrapper {
    display: flex;
    padding-top: 7px;
    justify-content: center;
    align-items: center;
}

.navbar-fixed {
    position: relative;
    z-index: 10;
    min-height: 25px;

    /* Make navbar draggable area in app.*/
    -webkit-app-region:drag;
    border-bottom: var(--container-darkest-border);
}

.dark .navbar-fixed{
    background-color: var(--gray-1);
}

.light .navbar-fixed{
    background-color:lightblue;
}

</style>

<script>

export default {
};
</script>
