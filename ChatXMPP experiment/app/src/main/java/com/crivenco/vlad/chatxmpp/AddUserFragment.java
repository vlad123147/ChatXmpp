package com.crivenco.vlad.chatxmpp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by vlad on 5/8/17.
 */

public class AddUserFragment extends DialogFragment {
    public static final String TAG = "ChatDialogFragment";

    public interface EditNameDialogListener {
        void onFinishEditDialog(String inputText);
    }

    private EditText editText;
    private EditNameDialogListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the EditNameDialogListener so we can send events to the host
            listener = (EditNameDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement EditNameDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.add_user_dialog_title);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.add_user_dialog_layout, null);
        builder.setView(v);

        editText = (EditText)v.findViewById(R.id.add_user_dialog_edit_text);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.e(TAG, "Add a new user");
                listener.onFinishEditDialog(editText.getText().toString());
                dismiss();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }

    private void setResult(String text){
        Intent i = new Intent();
        i.putExtra(ContactListActivity.EXTRA_NEW_CONTACT, text);
        getActivity().setResult(Activity.RESULT_OK, i);
    }
}
