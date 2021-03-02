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
    <div ref="vueTabs"></div>
</template>

<style scoped>

</style>

<script>
  import { Tabs, Tab } from '@blueprintjs/core';

  import ReactDom from 'react-dom';
  import React from 'react';

  export default {
    name: 'VueTabs',
    props: ['animate', 'defaultSelectedTabId', 'id', 'selectedTabId', 'vertical', 'tabs'],
    data() {
      return {
        tabsElement: null,
      };
    },
    watch: {
      tabs() {
        this.renderTabs();
        ReactDom.render(this.tabsElement, this.$refs.vueTabs);
      },
    },
    created() {
      this.renderTabs();
    },
    mounted() {
      this.$nextTick(() => {
        ReactDom.render(this.tabsElement, this.$refs.vueTabs);
      });
    },
    methods: {
      renderTabs() {
        const tabElements = this.tabs.map(x => React.createElement(Tab, {
          className: 'vue-tab',
          id: x,
          title: x,
          key: x,
        }));

        this.tabsElement = React.createElement(Tabs, {
          className: 'vue-tabs',
          animate: this.animate,
          defaultSelectedTabId: this.defaultSelectedTabId,
          id: this.id,
          selectedTabId: this.selectedTabId,
          vertical: this.vertical,
          onChange: (tab) => { this.$emit('tab-selected', tab); },
        }, tabElements);
      },
    },
  };
</script>

