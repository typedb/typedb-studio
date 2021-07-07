import React from 'react';
import '@testing-library/jest-dom';
import { render } from '@testing-library/react';
import { StudioApp } from '../app';

describe('StudioApp', () => {
    it('should render', () => {
        expect(render(<StudioApp/>)).toBeTruthy();
    });
});
