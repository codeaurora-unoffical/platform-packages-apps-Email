/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.email.activity.setup;

import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

import com.android.email.R;
import com.android.email.activity.UiUtilities;
import com.android.emailcommon.Logging;
import com.android.emailcommon.mail.Address;
import com.android.mail.utils.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParserException;

public class AccountSetupBasicsFragment extends AccountSetupFragment {
    private MultiAutoCompleteTextView mEmailView;
    private View mManualSetupView;
    private boolean mManualSetup;

    private ArrayList<String> mSuggestionList;

    public interface Callback extends AccountSetupFragment.Callback {
    }

    public static AccountSetupBasicsFragment newInstance() {
        return new AccountSetupBasicsFragment();
    }

    public AccountSetupBasicsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflateTemplatedView(inflater, container,
                R.layout.account_setup_basics_fragment, -1);

        mEmailView = (MultiAutoCompleteTextView)UiUtilities.getView(view, R.id.account_email);
        mManualSetupView = UiUtilities.getView(view, R.id.manual_setup);
        mManualSetupView.setOnClickListener(this);

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateFields();
            }
        };

        mEmailView.addTextChangedListener(textWatcher);

        setPreviousButtonVisibility(View.GONE);

        setManualSetupButtonVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getResources().getBoolean(R.bool.enable_auto_fill_domain)) {
            mSuggestionList = new ArrayList();
            initEmailAddressesArray();
            ArrayAdapter<String> addressAdapter = new ArrayAdapter<String>(
                    getActivity().getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line, mSuggestionList);
            mEmailView.setAdapter(addressAdapter);
            mEmailView.setThreshold(1);

            mEmailView.setTokenizer(new Tokenizer() {
                public int findTokenStart(CharSequence text, int cursor) {
                    int i = text.toString().indexOf('@');

                    if (i == -1) {
                        return cursor;
                    }
                    return i + 1;
                }

                public int findTokenEnd(CharSequence text, int cursor) {
                    int len = text.length();

                    return len;
                }

                public CharSequence terminateToken(CharSequence text) {
                    String mEmailString = mEmailView.getText().toString();
                    int indexOfAtTheRate = mEmailString.indexOf('@');
                    StringBuffer mEmailStringBuffer = new StringBuffer(mEmailString
                            .substring(0, (indexOfAtTheRate + 1)));
                    mEmailView.setText(mEmailStringBuffer.append(text));
                    mEmailView.setSelection(mEmailView.getText().length());
                    return "";
                }
            });
        }
    }

    private void initEmailAddressesArray() {
        try {
            parseXml(R.xml.providers);
            parseXml(R.xml.providers_product);
        } catch (XmlPullParserException e) {
            LogUtils.e(Logging.LOG_TAG, "XmlPullParserException while parsing xml ", e);
        } catch (IOException e) {
            LogUtils.e(Logging.LOG_TAG, "IOException while parsing xml ", e);
        }
    }

    /**
     * Validate the domain name, avoid domain name which contains
     * wild card characters and duplicates
     */
    private boolean validateDomain(String domain) {
        if ((domain != null) && (!(mSuggestionList.contains(domain)))
                    && (!domain.contains("*")) && (!domain.contains("?"))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parse xml file and add domain to suggestion list
     */
    private void parseXml(int xmlId) throws XmlPullParserException, IOException {
        String domain;
        int xmlEventType = 0;
        XmlResourceParser xml = getActivity().getBaseContext().getResources()
                .getXml(xmlId);

        while ((xmlEventType = xml.next())
                    != XmlResourceParser.END_DOCUMENT) {
            domain = xml.getAttributeValue(null, "domain");
            // Replace single wild card character with 'com'
            if ((domain != null)
                    && (domain.length() - domain.replace("*", "").length() == 1)) {
                domain = domain.replace("*", "com");
            }
            if (validateDomain(domain)) {
                mSuggestionList.add(domain);
            }
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        validateFields();
    }

    @Override
    public void onClick(View v) {
        final int viewId = v.getId();
        final Callback callback = (Callback) getActivity();

        if (viewId == R.id.next) {
            // Handle "Next" button here so we can reset the manual setup diversion
            mManualSetup = false;
            callback.onNextButton();
        } else if (viewId == R.id.manual_setup) {
            mManualSetup = true;
            callback.onNextButton();
        } else {
            super.onClick(v);
        }
    }

    private void validateFields() {
        final String emailField = getEmail();
        final Address[] addresses = Address.parse(emailField);

        /**
         * Default domain implementation
         * Verify default domain feaure is enable/disabled and username provided contains domian
         * if enabled, enable 'Next' and 'Manual' setup buttons
         * if disabled, set flow to base i.e disable 'Next' or 'Manual' setup button until
         * user provides domain name.
         */
        if (!getResources().getBoolean(R.bool.enable_auto_fill_domain)
                    || (getResources().getString(R.string.default_domain) == null)) {
            final boolean emailValid = !TextUtils.isEmpty(emailField)
                    && addresses.length == 1
                    && !TextUtils.isEmpty(addresses[0].getAddress());
            setNextButtonEnabled(emailValid);
        } else {
            boolean emailValid = false;
            if (!TextUtils.isEmpty(emailField) && emailField.contains("@")) {
                emailValid = !TextUtils.isEmpty(emailField)
                    && addresses.length == 1
                    && !TextUtils.isEmpty(addresses[0].getAddress());
            } else {
                emailValid = !TextUtils.isEmpty(emailField);
            }
            setNextButtonEnabled(emailValid);
        }

    }


    /**
     * Set visibitlity of the "manual setup" button
     * @param visibility {@link View#INVISIBLE}, {@link View#VISIBLE}, {@link View#GONE}
     */
    public void setManualSetupButtonVisibility(int visibility) {
        mManualSetupView.setVisibility(visibility);
    }

    @Override
    public void setNextButtonEnabled(boolean enabled) {
        super.setNextButtonEnabled(enabled);
        mManualSetupView.setEnabled(enabled);
    }

    public void setEmail(final String email) {
        mEmailView.setText(email);
    }

    public String getEmail() {
        return mEmailView.getText().toString().trim();
    }

    public boolean isManualSetup() {
        return mManualSetup;
    }
}
