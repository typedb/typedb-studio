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
    <div class="footer noselect">
        <div class="canvas-data">
            <div class="data-item no-of-entities"> entities: {{canvasData.entities}}</div>
            <div class="data-item no-of-attributes"> attributes: {{canvasData.attributes}}</div>
            <div class="data-item no-of-relations"> relations: {{canvasData.relations}}</div>
        </div>
    </div>
</template>
<style scoped>
    .footer {
        border-top: var(--container-darkest-border);
        background-color: var(--gray-1);
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
        z-index: 2;
        justify-content: flex-end;
    }

    .canvas-data {
        display: flex;
    }

    .data-item {
        justify-content: space-between;
        display: flex;
        margin-left: 5px;
    }
</style>

<script>
import { createNamespacedHelpers } from 'vuex';

export default {
  name: 'VisualiserFooter',
  props: ['tabId'],
  beforeCreate() {
    const { mapGetters } = createNamespacedHelpers(`tab-${this.$options.propsData.tabId}`);

    // computed
    this.$options.computed = {
      ...(this.$options.computed || {}),
      ...mapGetters(['canvasData']),
    };
  },
};
</script>
