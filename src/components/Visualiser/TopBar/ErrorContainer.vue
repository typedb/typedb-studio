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
    <div class="error-container z-depth-3">
        <div class="column">
            <div class="header noselect">ERROR</div>
            <div>{{errorMsg}}</div>
        </div>
        <div class="editor-tab">
            <div @click="$emit('close-error')"><vue-icon icon="cross" iconSize="12" className="tab-icon"></vue-icon></div>
            <div @click="copyError"><vue-icon icon="clipboard" iconSize="12" className="tab-icon"></vue-icon></div>
        </div>
    </div>
</template>

<style scoped>

    .column {
        display: flex;
        flex-direction: column;
        width: 100%;
        overflow-y: auto;
        padding: var(--container-padding);
    }

    .column::-webkit-scrollbar {
        width: 1px;
    }

    .column::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .editor-tab {
        max-height: 140px;
        width: 13px;
        flex-direction: column;
        display: flex;
        position: relative;
        float: right;
        border-left: 1px solid var(--red-4);
    }

    .error-container {
        background-color: var(--error-container-color);
        border: var(--container-darkest-border);
        width: 100%;
        max-height: 140px;
        margin-top: 10px;
        position: relative;
        white-space: pre-line;
        word-wrap: break-word;
        display: flex;
        flex-direction: row;
    }

    .header {
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 10px;
    }
</style>

<script>

  export default {
    name: 'ErrorContainer',
    props: ['errorMsg'],
    methods: {
      copyError() {
        // Create a dummy queryNameInput to copy the string array inside it
        const dummyInput = document.createElement('input');

        // Add it to the document
        document.body.appendChild(dummyInput);

        // Set its ID
        dummyInput.setAttribute('id', 'dummy_id');

        // Output the array into it
        document.getElementById('dummy_id').value = this.errorMsg;

        // Select it
        dummyInput.select();

        // Copy its contents
        document.execCommand('copy');

        // Remove it as its not needed anymore
        document.body.removeChild(dummyInput);

        this.$notifyInfo('Error message copied.');
      },
    },
  };
</script>
