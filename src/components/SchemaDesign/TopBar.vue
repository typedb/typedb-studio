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
    <div class="top-bar-container">
        <div class="left">
            <router-link to="/develop/data"><button class="btn"><vue-icon icon="eye-open" className="vue-icon"></vue-icon></button></router-link>
            <img src="img/grakn-workbase-logo.png" class="grakn-icon">
        </div>

        <div class="center">
            While Workbase is in schema design mode, the database will be unavailable for data write operations.
        </div>

        <div class="right">
            <databases-handler :showDatabaseTooltip="showDatabaseToolTip" v-on:database-selected="$emit('database-selected')"></databases-handler>
            <button class="btn" @click="$emit('toggle-preferences')"><vue-icon icon="cog" className="vue-icon"></vue-icon></button>
        </div>
    </div>
</template>

<style scoped>

    .top-bar-container {
        background-color: var(--gray-2);
        border-bottom: var(--container-darkest-border);
        width: 100%;
        height: 50px;
        position: relative;
        flex-direction: row;
        display: flex;
        justify-content: space-between;
        z-index: 2;
    }

    .left {
        padding: var(--container-padding);
        width: 140px;
        height: 100%;
        display: flex;
        align-items: center;
        vertical-align: middle;
        flex-direction: row;

        -webkit-touch-callout: none; /* iOS Safari */
        -webkit-user-select: none; /* Safari */
        -khtml-user-select: none; /* Konqueror HTML */
        -moz-user-select: none; /* Firefox */
        -ms-user-select: none; /* Internet Explorer/Edge */
        user-select: none; /* Non-prefixed version, currently
                                  supported by Chrome and Opera */
    }

    .grakn-icon {
        height: 25px;
        margin: 5px;
    }

    .center {
        padding: var(--container-padding);
        height: 100%;
        min-width: 800px;
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: center;
        vertical-align: middle;
    }

    .right {
        padding: var(--container-padding);
        width: 140px;
        height: 100%;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        }
</style>

<script>
    import DatabasesHandler from '../shared/DatabasesHandler.vue';

    export default {
      components: { DatabasesHandler },
      props: ['showDatabaseToolTip'],
    };
</script>
