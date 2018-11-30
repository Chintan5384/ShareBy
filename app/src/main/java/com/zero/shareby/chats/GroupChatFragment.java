package com.zero.shareby.chats;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.zero.shareby.DatabaseReferences;
import com.zero.shareby.R;

import java.util.ArrayList;

public class GroupChatFragment extends Fragment {
    private static final String TAG = "GroupChatFragment";
    ArrayList<Chat> chatsData;
    ChatsAdapter chatsAdapter;
    private ChildEventListener mListener=null;
    private DatabaseReference mChatRef;
    private DatabaseReference mGrpRef;
    EditText editMessage;

    public GroupChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView chats_list = view.findViewById(R.id.chats_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        chats_list.setHasFixedSize(true);
        chats_list.addItemDecoration(new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL));
        chats_list.setLayoutManager(layoutManager);

        chatsData =new ArrayList<>();
        chatsAdapter=new ChatsAdapter(chatsData);
        chats_list.setAdapter(chatsAdapter);
        mGrpRef = DatabaseReferences.getGroupReference(getContext());

        final ImageButton sendButton = view.findViewById(R.id.group_chat_send_message_button);

        editMessage = view.findViewById(R.id.group_chat_edit_text);
        sendButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        sendButton.setEnabled(false);
        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length()>0) {
                    sendButton.setEnabled(true);
                    sendButton.setBackgroundTintList(getResources().getColorStateList(R.color.sendButton));
                }
                else {
                    sendButton.setEnabled(false);
                    sendButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                editMessage.setText("");
            }
        });

    }


    private void attachChildListener(){
        if (mListener==null) {
            mListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        Chat getChatObject = dataSnapshot.getValue(Chat.class);
                        if (getChatObject.getSentBy().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                            getChatObject.setBelongsToCurrentUser(true);
                        else
                            getChatObject.setBelongsToCurrentUser(false);
                        chatsData.add(getChatObject);
                    }
                    chatsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
        }
        mChatRef.addChildEventListener(mListener);
    }

    private void sendMessage(){
        Chat createChatObj = new Chat(FirebaseAuth.getInstance().getCurrentUser().getUid(),null,editMessage.getText().toString(),System.currentTimeMillis());
        mChatRef.push().setValue(createChatObj).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG,"message Sent");
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mGrpRef == null){
            Toast.makeText(getContext(),"Grp not available",Toast.LENGTH_SHORT).show();
        }else {
            mChatRef = mGrpRef.child("chats");
            chatsAdapter.notifyDataSetChanged();
            Log.d(TAG, mChatRef.toString());
            attachChildListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mListener!=null){
            chatsData.clear();
            mChatRef.removeEventListener(mListener);
            mListener=null;
        }
    }
}
