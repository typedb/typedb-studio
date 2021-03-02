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
    <div ref="vueButton"></div>
</template>

<style scoped>

</style>

<script>
  import { Button } from '@blueprintjs/core';

  import ReactDom from 'react-dom';
  import React from 'react';

  export default {
    name: 'VueButton',
    props: ['text', 'icon', 'disabled', 'loading', 'rightIcon', 'className'],
    mounted() {
      this.$nextTick(() => {
        this.renderButton();
      });
    },
    watch: {
      className() {
        this.renderButton();
      },
      loading() {
        this.renderButton();
      },
      disabled() {
        this.renderButton();
      },
      icon() {
        this.renderButton();
      },
      text() {
        this.renderButton();
      },
    },
    methods: {
      renderButton() {
        ReactDom.render(React.createElement(Button, {
          text: this.text,
          icon: this.icon,
          loading: this.loading,
          rightIcon: this.rightIcon,
          className: this.className,
          disabled: this.disabled,
          onClick: (e) => { this.$emit('clicked', e); },
        }), this.$refs.vueButton);
      },
    },
  };
</script>

