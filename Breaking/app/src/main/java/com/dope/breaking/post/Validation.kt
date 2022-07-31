package com.dope.breaking.post

import android.util.Log
import android.view.View
import com.dope.breaking.databinding.ActivityPostBinding

class Validation {
    private val TAG = "Validation.kt" // Log Tag

    /**
     * @description - 게시글 작성 시에 글 제목과 내용, 가격은 사용자가 직접 필수로 입력해야 하는 항목이므로 요청 전에 비어 있지 않도록 검증하는 함수.
                      미디어 파일, 위치, 해시태그, 제보 방식, 익명 여부, 사건 발생 시간과, 썸네일 값은 기본 값이 존재하므로 굳이 검증하지 않음.
     * @param - binding(ActivityPostBinding)
     * @return - Boolean
     * @author - Tae Hyun Park
     * @since - 2022-07-31
     */
    fun startPostValidation(
        binding: ActivityPostBinding
    ):Boolean{
        // 한꺼번에 검증 처리가 안되는 문제로 따로 변수 할당
        val resultTitleValidation = titleValidation(binding)
        val resultContentValidation = contentValidation(binding)
        val resultPriceValidation = priceValidation(binding)
        return resultTitleValidation && resultContentValidation && resultPriceValidation // 세 가지가 다 제대로 입력되어야 true 반환
    }

    /**
     * @description - 글 제목 검증과 그에 따란 뷰 처리해주는 함수
     * @param - binding(ActivityPostBinding)
     * @return - Boolean
     * @author - Tae Hyun Park
     * @since - 2022-07-31
     */
    private fun titleValidation(binding: ActivityPostBinding): Boolean{
        return if (binding.etTitle.text.isNotBlank()) { // 글 제목이 입력되어 있다면
            binding.tvTitleError.visibility = View.GONE
            true
        }else if(binding.etTitle.text.isEmpty()){ // 비어져 있다면
            Log.d(TAG, "글 제목 비어 있음")
            binding.tvTitleError.visibility = View.VISIBLE
            binding.tvTitleError.text = "제목을 입력해야 합니다."
            false
        }else{
            Log.d(TAG, "글 제목 검증에서 예기지 못한 상황 발생")
            false
        }
    }

    /**
     * @description - 글 내용 검증과 그에 따른 뷰 처리해주는 함수
     * @param - binding(ActivityPostBinding)
     * @return - Boolean
     * @author - Tae Hyun Park
     * @since - 2022-07-31
     */
    private fun contentValidation(binding: ActivityPostBinding): Boolean{
        return if (binding.etContent.text.isNotBlank()) { // 글 내용이 입력되어 있다면
            binding.tvContentError.visibility = View.GONE
            true
        }else if(binding.etContent.text.isEmpty()){ // 비어져 있다면
            Log.d(TAG, "글 내용 비어 있음")
            binding.tvContentError.visibility = View.VISIBLE
            binding.tvContentError.text = "내용을 입력해야 합니다."
            false
        }else{
            Log.d(TAG, "글 내용 검증에서 예기지 못한 상황 발생")
            false
        }
    }

    /**
     * @description - 제보 가격 검증과 그에 따른 뷰 처리해주는 함수
     * @param - binding(ActivityPostBinding)
     * @return - Boolean
     * @author - Tae Hyun Park
     * @since - 2022-07-31
     */
    private fun priceValidation(binding: ActivityPostBinding): Boolean{
        return if (binding.etPostPrice.text.isNotBlank()) { // 제보 가격이 입력되어 있다면
            binding.tvPostError.visibility = View.GONE
            true
        }else if(binding.etPostPrice.text.isEmpty()){ // 비어져 있다면
            Log.d(TAG, "제보 가격 비어 있음")
            binding.tvPostError.visibility = View.VISIBLE
            binding.tvPostError.text = "가격을 입력해야 합니다."
            false
        }else{
            Log.d(TAG, "예기지 못한 상황 발생")
            false
        }
    }
}