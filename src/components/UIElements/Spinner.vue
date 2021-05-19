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
<div v-if="showSpinner" :class= "className">
    <svg class="spinner-circle" viewBox="0 0 66 66">
        <circle class="path" fill="none" stroke-width="6" stroke-linecap="round" cx="33" cy="33" r="30"></circle>
    </svg>
</div>
</template>
<script>
export default {
  props: ['className'],
  computed: {
    showSpinner() { return this.$store.getters.showSpinner; },
  },
};
</script>

<style lang="scss" scoped>

$offset: 187;
$duration: 1.4s;
.spinner-data {
  animation: circle-rotator $duration linear infinite;
  * {
    line-height: 0;
    box-sizing: border-box;
  }
  width: 40px;
  height: 40px;
  z-index: 2;
  position: absolute;
  top: 120%;
  right: 150px;
}

.spinner-schema {
  animation: circle-rotator $duration linear infinite;
  * {
    line-height: 0;
    box-sizing: border-box;
  }
  width: 40px;
  height: 40px;
  z-index: 2;
  top: 4%;
  right: 20%;
  position: absolute;
}

@keyframes circle-rotator {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(270deg); }
}
.path {
  stroke-dasharray: $offset;
  stroke-dashoffset: 0;
  transform-origin: center;
  animation:
          circle-dash $duration ease-in-out infinite,
          circle-colors ($duration*4) ease-in-out infinite;
}
@keyframes circle-colors {
  0% {
    stroke: #35495e;
  }
  25% {
    stroke: #DE3E35;
  }
  50% {
    stroke: #F7C223;
  }
  75% {
    stroke: #41b883;
  }
  100% {
    stroke: #35495e;
  }
}
@keyframes circle-dash {
  0% {
    stroke-dashoffset: $offset;
  }
  50% {
    stroke-dashoffset: $offset/4;
    transform:rotate(135deg);
  }
  100% {
    stroke-dashoffset: $offset;
    transform:rotate(450deg);
  }
}
</style>