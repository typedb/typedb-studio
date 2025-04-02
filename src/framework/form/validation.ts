/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AbstractControl, FormControl, FormGroup, ValidatorFn, Validators } from "@angular/forms";

export const requiredValidator: ValidatorFn = (control: AbstractControl) => {
    return Validators.required(control) === null ? null : { errorText: "Required" };
};

export const rangeValidator: (min: number, max?: number) => ValidatorFn = (min, max) => ((control: AbstractControl) => {
    const formControl = control as FormControl<number>;
    if (formControl.value < min) return { errorText: `Must be at least ${min}` };
    if (typeof max === "number" && formControl.value > max) return { errorText: `Must be at most ${max}` };
    return null;
});

export const patternValidator: (pattern: RegExp, errorText: string) => ValidatorFn = (pattern, errorText) => ((control: AbstractControl) => {
    const formControl = control as FormControl<string>;
    return !formControl.value.trim().length || pattern.test(formControl.value) ? null : { errorText };
});

export const nonEmptyValidator: (errorText: string) => ValidatorFn = (errorText) => ((control: AbstractControl) => {
    return Validators.required(control) === null ? null : { errorText };
});

export const passwordValidator: ValidatorFn = (control: AbstractControl) => {
    const formControl = control as FormControl<string>;
    const password = formControl.value;
    const specialCharacters = "[ !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~]";
    return (
        password.length >= 8 &&
        password.toLowerCase() !== password &&
        password.toUpperCase() !== password &&
        (password.match(new RegExp("\\d")) ?? []).length > 0 &&
        (password.match(new RegExp(specialCharacters)) ?? []).length > 0
    ) ? null : { errorText: "Must contain at least 8 characters; mixing uppercase, lowercase, numbers, and special characters\n" };
}

export const confirmPasswordValidator: ValidatorFn = ((control: AbstractControl) => {
    const formGroup = control as FormGroup<{ password: FormControl<string>, confirmPassword: FormControl<string> }>;
    return formGroup.controls.confirmPassword.touched && formGroup.value.password !== formGroup.value.confirmPassword
        ? { errorText: "Password and confirm password must match" }
        : null;
});
