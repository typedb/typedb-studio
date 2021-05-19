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
    <div class="icon icon--caret" :class="{'toggle--north':toggleNorth}">
        <span class="line--vertical"></span>
        <span class="line--horizontal"></span>
    </div>
</template>
<script>

export default {
  name: 'CaretIcon',
  props: ['toggleNorth'],
};
</script>
<style lang="scss" scoped>

.icon {
  display: inline-block;
  position: relative;
  cursor: pointer;
  padding: 12px;
  margin-left: auto;
  transition: 100ms;

  &.icon--caret {
   
    &::before {
      position: absolute;
      content: '';
      width: 1px;
      height: 10px;
      background: white;
      display: inline-block;
      transition: 300ms;
      top: 30%;
      left: 32%;
      transform: rotateZ(45deg);
    }

    &::after {
      position: absolute;
      content: '';
      width: 1px;
      height: 10px;
      background: white;
      display: inline-block;
      transition: 300ms;
      top: 30%;
      left: 3%;
      transform: rotateZ(-45deg);
    }
    
    &.toggle--north {
      &::before {
        transform: rotateZ(-45deg);
      }
      &::after {
        transform: rotateZ(45deg);
      }
    }
  }
}
</style>
