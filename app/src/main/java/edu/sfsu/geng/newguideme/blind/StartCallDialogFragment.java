package edu.sfsu.geng.newguideme.blind;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import edu.sfsu.geng.newguideme.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StartCallDialogListener} interface
 * to handle interaction events.
 */
public class StartCallDialogFragment extends DialogFragment {

    private StartCallDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.start_call_dialog_layout, null);
        final EditText descriptionEditText = (EditText) dialogView.findViewById(R.id.start_call_des_string);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.start_call_hint)
                .setView(dialogView)
                .setPositiveButton(R.string.start_call_next, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String description = descriptionEditText.getText().toString();
                        mListener.onDescriptionInput(description);
                    }
                })
                .setNegativeButton(R.string.start_call_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (StartCallDialogListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString()
                    + " must implement StartCallDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface StartCallDialogListener {
        void onDescriptionInput(String desString);
    }
}
