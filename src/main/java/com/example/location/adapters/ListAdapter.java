package com.example.location.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.location.Comments;
import com.example.location.MainActivity;
import com.example.location.R;
import com.example.location.Settings;
import com.google.firebase.firestore.GeoPoint;

import java.util.LinkedList;
import java.util.Map;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.WordViewHolder> {

    private final LinkedList< Map> CommentList;
    private final LayoutInflater mInflater;
    private final Context adapter;
    public static final String POSITION_FROM_COMMENTS = "Position_From_Comments";

    class WordViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener/*, View.OnLongClickListener*/ {

        public final TextView CommentItemIdView;
        public final TextView CommentItemBodyView;
        public final TextView CommentItemTitleView;
        public final TextView CommentItemVisibility;
        final ListAdapter mAdapter;

        public WordViewHolder(View itemView, ListAdapter adapter) {
            super(itemView);
            CommentItemIdView = itemView.findViewById(R.id.Comment_Id_Rec);
            CommentItemTitleView = itemView.findViewById(R.id.Comment_Title_Rec);
            CommentItemBodyView = itemView.findViewById(R.id.Comment_Body_Rec);
            CommentItemVisibility = itemView.findViewById( R.id.Comment_Visible_Rec);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this);
            //itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Get the position of the item that was clicked.
            //se sono nella lista dei commenti attorno a me e ci premo sopra mi rimanda sulla mappa
            if( adapter == Comments.commentsContext){
                int mPosition = getLayoutPosition();
                Log.d( "ListAdapter", "onClick : position : " + mPosition);
                Intent pointInMap = new Intent( adapter, MainActivity.class);
                pointInMap.setAction( "Position_Selected_By_Recycler");
                Map< String, Object> singleComment = CommentList.get( mPosition);
                GeoPoint commentPosition = ( GeoPoint) singleComment.get( "Geo_Point");
                Comments local = ( Comments) Comments.commentsContext;
                local.finishComments( commentPosition.getLatitude(), commentPosition.getLongitude());
            }
            //se sono sulla lista dei commenti di un profilo e ci premo sopra lo metto visible o non visible
            if( adapter == Settings.settingsContext){
                Log.d( "Settings", "onClick : position : " + getLayoutPosition());
                Map< String, Object> buffer = CommentList.get( getLayoutPosition());
                CommentList.remove( getLayoutPosition());
                String visibility = (String) buffer.get("Visible");
                Log.d( "Settings", "onClick : Visible : " + visibility);
                if( visibility.equals("True")) {
                    MainActivity.database.updateCommentVisibility((String) buffer.get("Document_Id"), false, visibility.equals("True"));
                    CommentItemVisibility.setText( "Invisible");
                    Toast.makeText(Settings.settingsContext,"Your comment is now invisible",Toast.LENGTH_SHORT).show();
                }
                else {
                    MainActivity.database.updateCommentVisibility((String) buffer.get("Document_Id"), true, visibility.equals("True"));
                    CommentItemVisibility.setText("Visible");
                    Toast.makeText(Settings.settingsContext,"Your comment is now visible",Toast.LENGTH_SHORT).show();
                }
                buffer.put( "Visible", "False");
                CommentList.add( getLayoutPosition(), buffer);
            }
        }

        /*@Override
        public boolean onLongClick(View v) {
            if( adapter == Settings.settingsContext){
                int mPosition = getLayoutPosition();
                Log.d( "ListAdapter", "onClick : position : " + mPosition);
                Intent pointInMap = new Intent( adapter, MainActivity.class);
                pointInMap.setAction( "Position_Selected_By_Recycler");
                Map< String, Object> singleComment = CommentList.get( mPosition);
                GeoPoint commentPosition = ( GeoPoint) singleComment.get( "Geo_Point");
                Intent positionFromComments = new Intent(POSITION_FROM_COMMENTS);
                Bundle selectedCommentMarker = new Bundle();
                positionFromComments.putExtra( MainActivity.LATITUDE_FROM_COMMENTS, commentPosition.getLatitude());
                positionFromComments.putExtra( MainActivity.LONGITUDE_FROM_COMMENTS, commentPosition.getLongitude());
                pointInMap.putExtra( "Position_Selected_Comment", selectedCommentMarker);
                adapter.startActivity( pointInMap);
                adapter.sendBroadcast( positionFromComments);
                Comments local = ( Comments)Comments.commentsContext;
                local.finishComments( commentPosition.getLatitude(), commentPosition.getLongitude());
            }
            return false;
        }*/
    }


    public ListAdapter(Context context, LinkedList< Map> wordList) {
        mInflater = LayoutInflater.from(context);
        adapter = context;
        this.CommentList = wordList;
    }

    @Override
    public ListAdapter.WordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate an item view.
        View mItemView = mInflater.inflate( R.layout.list_item, parent, false);
        return new WordViewHolder( mItemView, this);
    }

    @Override
    public void onBindViewHolder(ListAdapter.WordViewHolder holder, int position) {

        Map<String, Object> mCurrent = CommentList.get(position);
        // Add the data to the view holder.
        holder.CommentItemIdView.setText( (String)mCurrent.get( "Name") + "  " + (String)mCurrent.get( "Date"));
        if( mCurrent.get("Visible").equals("True")){
            holder.CommentItemVisibility.setVisibility( View.INVISIBLE);
        }
        else{
            holder.CommentItemVisibility.setVisibility( View.VISIBLE);
            holder.CommentItemVisibility.setText( "Invisible");
        }
        holder.CommentItemTitleView.setText( (String)mCurrent.get( "Title"));
        holder.CommentItemBodyView.setText( (String)mCurrent.get( "Body"));

    }

    @Override
    public int getItemCount() {
        return CommentList.size();
    }
}