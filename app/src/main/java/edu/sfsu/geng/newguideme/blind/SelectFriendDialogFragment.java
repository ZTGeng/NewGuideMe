package edu.sfsu.geng.newguideme.blind;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.Set;

import edu.sfsu.geng.newguideme.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SelectFriendDialogFragment extends DialogFragment {

    private SelectFriendDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SelectFriendDialogListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString()
                    + " must implement SelectFriendDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Set<String> friends = mListener.getFriends();
        final String[] friendArray = friends.toArray(new String[friends.size()]);
        final boolean[] friendChecked = new boolean[friends.size()];

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_friend)
                .setMultiChoiceItems(friendArray, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        friendChecked[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.start_call_next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<String> friendList = new ArrayList<>();
                        for (int i = 0; i < friendArray.length; i++) {
                            if (friendChecked[i]) {
                                friendList.add(friendArray[i]);
                            }
                        }
                        mListener.onFriendSelect(friendList);
                    }
                })
                .setNegativeButton(R.string.start_call_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }

    public interface SelectFriendDialogListener {
        void onFriendSelect(ArrayList<String> selectedFriends);
        Set<String> getFriends();
    }
}
