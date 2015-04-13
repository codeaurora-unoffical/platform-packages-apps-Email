/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.activity;

import android.widget.EditText;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import com.android.email.R;

public class UiUtilities {
    private UiUtilities() {
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    public static void maxLengthFilter(final Context context, final EditText editText,
            final int maxLength) {
        InputFilter[] contentFilters = new InputFilter[1];
        contentFilters[0] = new InputFilter.LengthFilter(maxLength) {
            public CharSequence filter(CharSequence source, int start, int end,
                    Spanned dest, int dstart, int dend) {
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null) {
                    showWarningToast(context, maxLength);
                }
                return result;
            }
        };
        editText.setFilters(contentFilters);
    }

    public static void showWarningToast(final Context context, int maxLength) {
        Toast.makeText(context, context.getString(R.string.max_input, maxLength),
                Toast.LENGTH_SHORT).show();
    }
}
