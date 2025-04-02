/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

export interface MenuItem {
    label: string;
    action: () => void;
    checkbox?: boolean;
    disabled?: boolean;
    disabledReason?: string;
    dangerous?: boolean;
}
