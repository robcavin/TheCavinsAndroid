package com.bumblebeejuice.thecavinsfinal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;

public class PostActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        HttpTask helloCall = new HttpTask() {
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                super.onPostExecute(jsonObject);

                if (jsonObject != null) Log.d(this.getClass().getName(), jsonObject.toString());
            }
        };
        helloCall.execute("/hello");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final int SELECT_PHOTO = 100;
        private static final int CROP_PHOTO = 101;

        private EditText _postText = null;
        private String _postImagePath = null;
        private ImageButton _imageButton = null;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_post, container, false);

            _postText = (EditText) rootView.findViewById(R.id.editText);
            _postText.setImeOptions(EditorInfo.IME_ACTION_DONE);

            _postText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(_postText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        handled = true;
                    }
                    return handled;
                }
            });

            _imageButton = (ImageButton) rootView.findViewById(R.id.imageButton);
            _imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    File dir = getActivity().getExternalCacheDir();
                    if (dir == null) {
                        dir = getActivity().getCacheDir();
                    }

                    File f = new File(dir,"temp.jpg");
                    try {
                        f.createNewFile();
                    } catch (IOException ex) {
                        Log.e("io", ex.getMessage());
                    }

                    final Uri uri = Uri.fromFile(f);
                    _postImagePath = f.getAbsolutePath();

                    String title = "Select an Image";
                    CharSequence[] itemlist = {
                            "Take a Photo",
                            "Pick from Gallery"
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(title);
                    builder.setItems(itemlist, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;

                            switch (which) {
                                case 0:// Take Photo
                                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                                    intent.putExtra("crop", "true");
                                    intent.putExtra("aspectX", 512); // This sets the max width.
                                    intent.putExtra("aspectY", 512); // This sets the max height.
                                    intent.putExtra("outputX", 512);
                                    intent.putExtra("outputY", 512);
                                    intent.putExtra("scale", true);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                    intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                                    startActivityForResult(intent, SELECT_PHOTO);
                                    break;

                                case 1:// Choose Existing Photo

                                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("image/*");

                                    intent.putExtra("crop", "true");
                                    intent.putExtra("aspectX", 512); // This sets the max width.
                                    intent.putExtra("aspectY", 512); // This sets the max height.
                                    intent.putExtra("outputX", 512);
                                    intent.putExtra("outputY", 512);
                                    intent.putExtra("scale", true);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                    intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                                    startActivityForResult(intent, SELECT_PHOTO);
                                    break;

                                default:
                                    break;
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.setCancelable(true);
                    alert.show();

                }
            });

            Button postButton = (Button) rootView.findViewById(R.id.postButton);
            postButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (_postText.getText().toString() == "") {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("Oops")
                                .setTitle("Please add a message before submitting.");
                        builder.setNegativeButton("Cancel", null);
                        builder.create().show();
                    } else {
                        HttpTask apiCall = new HttpTask() {
                            @Override
                            protected void onPostExecute(JSONObject jsonObject) {
                                super.onPostExecute(jsonObject);

                                if (jsonObject != null)
                                    Log.d(this.getClass().getName(), jsonObject.toString());
                            }
                        };

                        apiCall.setMethod("POST");

                        HashMap<String, String> args = new HashMap<String, String>();
                        args.put("text", _postText.getText().toString());
                        apiCall.setArgs(args);

                        ArrayList<HashMap<String, Object>> files = new ArrayList<HashMap<String, Object>>();
                        if (_postImagePath != null) {
                            HashMap<String, Object> fileInfo = new HashMap<String, Object>();

                            try {
                                fileInfo.put("path", _postImagePath);
                                fileInfo.put("name", "image");
                                fileInfo.put("filename", "image.jpg");
                                fileInfo.put("mime-type", "image/jpeg");
                                files.add(fileInfo);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        apiCall.setFiles(files);

                        apiCall.execute("/stream/1/post");
                    }
                }
            });

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                                     Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);

            switch (requestCode) {
                case SELECT_PHOTO:
                    if (resultCode == RESULT_OK) {
                        Bitmap photo = BitmapFactory.decodeFile(_postImagePath);
                        _imageButton.setImageBitmap(photo);
                    }
            }
        }
    }


}

/*
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getActivity().getContentResolver().query(imageUri, filePathColumn, null, null, null);
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        cursor.close();

                        _postImagePath = filePath;

                        Bitmap image = BitmapFactory.decodeFile(filePath);
                        _imageButton.setImageBitmap(image);

 */