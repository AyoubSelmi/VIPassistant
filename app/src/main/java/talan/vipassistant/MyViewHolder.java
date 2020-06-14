package talan.vipassistant;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class MyViewHolder  extends RecyclerView.ViewHolder {
TextView item_description;
    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
        item_description=(TextView)itemView.findViewById(R.id.item_description);
    }
}
