<!--
 Copyright (C) 2021 Vaticle

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
  <div class="graph-panel-body">
    <div id="graph-div" ref="graph"></div>
  </div>
</template>

<style lang="scss" scoped>
  .graph-panel-body {
    height: 100%;
    width: 100%;
    position: absolute;
  }
  .graph-panel-body * {
      -webkit-touch-callout: none;
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
  }

  #graph-div {
    height: 100%;
  }
</style>

<script>
import { createNamespacedHelpers } from 'vuex';

import VisFacade from '@/components/CanvasVisualiser/Facade';
import { INITIALISE_VISUALISER } from './StoresActions';

export default {
  name: 'GraphCanvas',
  props: ['tabId'],
  beforeCreate() {
    const namespace = (this.$route.path === '/develop/data') ? `tab-${this.$options.propsData.tabId}` : 'schema-design';
    const { mapActions } = createNamespacedHelpers(namespace);

    // methods
    this.$options.methods = {
      ...(this.$options.methods || {}),
      ...mapActions([INITIALISE_VISUALISER]),
    };
  },
  mounted() {
    this.$nextTick(() => {
      const graphDiv = this.$refs.graph;
      this[INITIALISE_VISUALISER]({ container: graphDiv, visFacade: VisFacade });
    });
  },
};
</script>
