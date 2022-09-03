package com.dope.breaking.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dope.breaking.R
import com.dope.breaking.model.response.ResponseMainFeed
import com.dope.breaking.model.response.ResponseTransaction
import com.dope.breaking.util.ValueUtil
import java.text.DecimalFormat

class TransactionAdapter(
    private val context: Context,
    val data: MutableList<ResponseTransaction?>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val selfType = 0 // 충전/출금 레이아웃
    private val otherType = 2 // 구매/판매 레이아웃
    private val decimalFormat = DecimalFormat("#,###") // 숫자 포맷
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            selfType -> {
                val view =
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_transaction_self, parent, false)
                SelfViewHolder(view)
            }
            otherType -> {
                val view =
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_transaction_other, parent, false)
                OtherViewHolder(view)
            }
            else -> { // 로딩 레이아웃
                val view =
                    LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SelfViewHolder) {
            holder.bind(data[position]!!)
        } else if (holder is OtherViewHolder) {
            holder.bind(data[position]!!)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    /**
     * View 타입
     * 1. 데이터가 null 이면 로딩 아이템
     * 2. 데이터의 거래 타입이 출금/입금 이면 self 아이템
     * 3. 데이터의 거래 타입이 구매/판매 이면 other 아이템
     */
    override fun getItemViewType(position: Int): Int {
        return if (data[position] == null) ValueUtil.VIEW_TYPE_LOADING
        else
            if (data[position]!!.transactionType == "withdraw"
                || data[position]!!.transactionType == "deposit"
            ) selfType
            else otherType
    }

    inner class SelfViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val method = itemView.findViewById<TextView>(R.id.tv_self_transaction_method)
        private val date = itemView.findViewById<TextView>(R.id.tv_self_transaction_date)
        private val variable = itemView.findViewById<TextView>(R.id.tv_self_transaction_var)
        private val balance = itemView.findViewById<TextView>(R.id.tv_self_transaction_balance)

        fun bind(item: ResponseTransaction) {
            date.text = item.transactionDate.replace("T", " ").split(".")[0]
            balance.text = decimalFormat.format(item.balance) + "원"
            if (item.transactionType == "withdraw") {
                method.text = "출금"
                method.setTextColor(context.getColor(R.color.sign_up_input_error_text_color))
                variable.text = "-${decimalFormat.format(item.amount)}원"
                variable.setTextColor(context.getColor(R.color.sign_up_input_error_text_color))
            } else {
                method.text = "입금"
                method.setTextColor(context.getColor(R.color.breaking_color))

                variable.text = "+${decimalFormat.format(item.amount)}원"
                variable.setTextColor(context.getColor(R.color.breaking_color))
            }

        }
    }

    inner class OtherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val method = itemView.findViewById<TextView>(R.id.tv_other_transaction_method)
        private val title = itemView.findViewById<TextView>(R.id.tv_other_transaction_title)
        private val date = itemView.findViewById<TextView>(R.id.tv_other_transaction_date)
        private val variable = itemView.findViewById<TextView>(R.id.tv_other_transaction_var)
        private val balance = itemView.findViewById<TextView>(R.id.tv_other_transaction_balance)

        fun bind(item: ResponseTransaction) {
            date.text = item.transactionDate.replace("T", " ").split(".")[0]
            balance.text = decimalFormat.format(item.balance) + "원"
            if (item.transactionType == "buy_post") {
                method.text = "구매"
                method.setTextColor(context.getColor(R.color.sign_up_input_error_text_color))
                variable.text = "-${decimalFormat.format(item.amount)}원"
                variable.setTextColor(context.getColor(R.color.sign_up_input_error_text_color))
            } else {
                method.text = "판매"
                method.setTextColor(context.getColor(R.color.breaking_color))

                variable.text = "+${decimalFormat.format(item.amount)}원"
                variable.setTextColor(context.getColor(R.color.breaking_color))
            }
            val tmp =
                if (item.transactionType == "buy_post") "${item.targetUser!!.nickname}님의 \'${item.postTitle}\' 구매"
                else "${item.targetUser!!.nickname}님에게 \'${item.postTitle}\' 판매"

            val spannableString = SpannableString(tmp)
            val target1 = tmp.indexOf(item.targetUser.nickname)
            val target2 = tmp.indexOf(item.postTitle!!)
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                target1,
                target1 + item.targetUser.nickname.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                target2,
                target2 + item.postTitle.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            title.text = spannableString
        }
    }

    inner class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val progressDialog = itemView.findViewById<ProgressBar>(R.id.progressbar_loading)
    }

    /**
     * 아이템 리스트를 리스트에 추가하는 함수
     * @param items(List<ResponseTransaction>): 입출금 내역 객체 리스트
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    fun addItems(items: List<ResponseTransaction>) {
        data.addAll(items)
        notifyItemRangeInserted(itemCount, items.size)
    }

    /**
     * 아이템 하나를 리스트에 추가하는 함수
     * @param item(ResponseTransaction?): 입출금 아이템 객체 하나 (nullable)
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    fun addItem(item: ResponseTransaction?) {
        data.add(item)
        notifyItemInserted(itemCount)
    }

    /**
     * 리스트의 마지막 아이템을 지우는 함수
     * @author Seunggun Sin
     * @since 2022-09-03
     */
    fun removeLast() {
        data.removeAt(data.size - 1)
        notifyItemRemoved(data.size)
    }

}