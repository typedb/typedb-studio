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
    <div ref="vueIcon"></div>
</template>

<style scoped>

</style>

<script>
  import { Icon } from '@blueprintjs/core';

  import ReactDom from 'react-dom';
  import React from 'react';

  export default {
    name: 'VueIcon',
    props: ['color', 'icon', 'iconSize', 'intent', 'title', 'className'],
    mounted() {
      this.$nextTick(() => {
        this.renderIcon();
      });
    },
    watch: {
      icon() {
        this.renderIcon();
      },
      iconSize() {
        this.renderIcon();
      },
    },
    methods: {
      renderIcon() {
        ReactDom.render(React.createElement(Icon, {
          className: this.className,
          color: this.color,
          icon: this.icon,
          iconSize: this.iconSize,
          intent: this.intent,
          title: this.title,
        }), this.$refs.vueIcon);
      },
    },
  };
</script>

