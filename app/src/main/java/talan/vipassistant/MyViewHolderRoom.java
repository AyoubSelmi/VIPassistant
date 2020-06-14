package talan.vipassistant;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class MyViewHolderRoom  extends RecyclerView.ViewHolder {
    TextView room_description;
    public MyViewHolderRoom(@NonNull View itemView) {
        super(itemView);
        room_description=(TextView)itemView.findViewById(R.id.room_description);
    }
}
