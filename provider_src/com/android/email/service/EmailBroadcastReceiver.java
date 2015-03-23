/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.email.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.email.R;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import android.util.Log;

/**
 * The broadcast receiver. The actual job is done in EmailBroadcastProcessor on a worker thread.
 */
public class EmailBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        EmailBroadcastProcessorService.processBroadcastIntent(context, intent);
    }

    public static class MailServiceReceiver extends BroadcastReceiver {
        private static final String TAG = "MailServiceReceiver";
        private static final String ACTION_CHECK_MAIL =
                "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
        private static final String ACTION_DELETE_MESSAGE =
                "com.android.email.intent.action.MAIL_SERVICE_DELETE_MESSAGE";
        private static final String ACTION_MOVE_MESSAGE =
                "com.android.email.intent.action.MAIL_SERVICE_MOVE_MESSAGE";
        private static final String ACTION_MESSAGE_READ =
                "com.android.email.intent.action.MAIL_SERVICE_MESSAGE_READ";
        private static final String ACTION_SEND_PENDING_MAIL =
                "com.android.email.intent.action.MAIL_SERVICE_SEND_PENDING";

        private static final String EXTRA_ACCOUNT =
                "com.android.email.intent.extra.ACCOUNT";
        private static final String EXTRA_MESSAGE_ID =
                "com.android.email.intent.extra.MESSAGE_ID";
        private static final String EXTRA_MESSAGE_INFO =
                "com.android.email.intent.extra.MESSAGE_INFO";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);

            Intent startIntent = null;
            if (ACTION_CHECK_MAIL.equals(action)) {
                final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
                final long inboxId = Mailbox.findMailboxOfType(context, accountId,
                        Mailbox.TYPE_INBOX);
                Log.d(TAG, "accountId is " + accountId + ", inboxId is " + inboxId);

                Mailbox mailbox = Mailbox.restoreMailboxWithId(context, inboxId);
                if (mailbox == null) return;
                Account account = Account.restoreAccountWithId(context, mailbox.mAccountKey);
                if (account == null) return;

                String protocol = account.getProtocol(context);
                String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
                Log.d(TAG, "protocol is " + protocol);
                if (protocol.equals(legacyImapProtocol)) {
                    startIntent = new Intent(context, ImapService.class);
                } else {
                    startIntent = new Intent(context, Pop3Service.class);
                }
                startIntent.setAction(action);
                startIntent.putExtra(EXTRA_ACCOUNT, accountId);
            } else if (ACTION_DELETE_MESSAGE.equals(action)) {
                final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
                Log.d(TAG, "messageId is " + messageId);
                Account account = Account.getAccountForMessageId(context, messageId);
                if (account == null) return;

                String protocol = account.getProtocol(context);
                Log.d(TAG, "protocol is " + protocol + ", accountId: " + account.mId);
                String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
                if (protocol.equals(legacyImapProtocol)) {
                    startIntent = new Intent(context, ImapService.class);
                    startIntent.setAction(action);
                    startIntent.putExtra(EXTRA_ACCOUNT, account.mId);
                    startIntent.putExtra(EXTRA_MESSAGE_ID, messageId);
                } else {
                    Log.w(TAG, "DELETE MESSAGE POP3 NOT Implemented");
                }
            } else if (ACTION_MESSAGE_READ.equals(action)
                    || ACTION_MOVE_MESSAGE.equals(action)) {
                final int messageInfo = intent.getIntExtra(EXTRA_MESSAGE_INFO, 0);
                final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
                Log.d(TAG, "messageId is " + messageId);
                Account account = Account.getAccountForMessageId(context, messageId);
                if (account == null) return;

                String protocol = account.getProtocol(context);
                Log.d(TAG, "protocol is " + protocol + ", accountId: " + account.mId);
                String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
                if (protocol.equals(legacyImapProtocol)) {
                    startIntent = new Intent(context, ImapService.class);
                    startIntent.setAction(action);
                    startIntent.putExtra(EXTRA_ACCOUNT, account.mId);
                    startIntent.putExtra(EXTRA_MESSAGE_ID, messageId);
                    startIntent.putExtra(EXTRA_MESSAGE_INFO, messageInfo);
                } else {
                    Log.w(TAG, "READ OR MOVE MESSAGE POP3 NOT Implemented");
                }
            } else if (ACTION_SEND_PENDING_MAIL.equals(action)) {
                final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
                Log.d(TAG, "accountId is " + accountId);
                Account account = Account.restoreAccountWithId(context, accountId);
                if (account == null) return;

                String protocol = account.getProtocol(context);
                Log.d(TAG, "protocol is " + protocol);
                String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
                if (protocol.equals(legacyImapProtocol)) {
                    startIntent = new Intent(context, ImapService.class);
                    startIntent.setAction(action);
                    startIntent.putExtra(EXTRA_ACCOUNT, accountId);
                } else {
                    Log.w(TAG, "SEND MESSAGE POP3 NOT Implemented");
                }
            }

            if (startIntent != null) context.startService(startIntent);
        }
    }
}
