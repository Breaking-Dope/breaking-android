package com.dope.breaking.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.animation.ObjectAnimator
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dope.breaking.BuildConfig
import com.dope.breaking.R
import com.dope.breaking.databinding.ActivityKakaoMapBinding
import com.dope.breaking.exception.FailedGetLocationException
import com.dope.breaking.exception.ResponseErrorException
import com.dope.breaking.model.LocationList
import com.dope.breaking.model.response.ResponseLocationSearch
import com.dope.breaking.retrofit.RetrofitManager
import com.dope.breaking.retrofit.RetrofitService
import com.dope.breaking.util.DialogUtil
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KakaoMapActivity : AppCompatActivity() {

    private val TAG = "KakaoMapActivity.kt"

    private var mbinding : ActivityKakaoMapBinding? = null

    private val binding get() = mbinding!!

    private val listItems = arrayListOf<LocationList>()   // 리사이클러 뷰 아이템

    private val listAdapter = MapListAdapter(listItems)    // 리사이클러 뷰 어댑터

    private var isFabOpen = false // 플로팅 버튼 액션 메뉴 열림 여부

    private var address: List<Address>? = null // 현재 위치를 담을 Address 타입의 List

    private var uLatitude:Double = 0.0 // 현재 위치의 위도

    private var uLongitude:Double = 0.0 // 현재 위치의 경도

    private var pageNumber = 1      // 검색 페이지 번호

    private var keyword = ""        // 검색 키워드

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = ActivityKakaoMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ibNextPage.isEnabled = false   // 다음 페이지 버튼 비활성화
        binding.ibPrevPage.isEnabled = false   // 이전 페이지 버튼 비활성화

        // 리사이클러 뷰
        binding.rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvList.adapter = listAdapter

        requestPermission() // 유저에게 위치 권한 요청
        clickMapPageButtons() // 클릭 모음 함수
    }

    /**
     * @description - 카카오 맵 페이지 클릭 관련 콜백 함수를 모아놓은 메소드
     * @param - None
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-10
     */
    private fun clickMapPageButtons() {
        // 리스트 아이템 클릭 시 해당 위치로 이동
        listAdapter.setItemListClickListener(object: MapListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                closeFloatingButton() // 플로팅 버튼 닫기 & 현재 위치 추적 중지
                val mapPoint = MapPoint.mapPointWithGeoCoord(listItems[position].y, listItems[position].x)
                binding.viewMap.setMapCenterPointAndZoomLevel(mapPoint, 1, true) // 해당 위치로 이동
            }
        })

        // 리스트 아이템 선택 버튼 클릭 시 제보하기 페이지로 데이터 전달
        listAdapter.setItemButtonClickListener(object : MapListAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int) {
                var bundle = Bundle()
                bundle.putSerializable("locationData", listItems[position])
                intent.putExtras(bundle) // 위치 데이터 전달
                setResult(RESULT_OK, intent)
                finish() // 이전 액티비티로 이동
            }
        })

        // 검색 버튼
        binding.btnSearch.setOnClickListener {
            closeFloatingButton() // 플로팅 버튼 닫기 & 현재 위치 추적 중지
            keyword = binding.etSearchField.text.toString()
            pageNumber = 1
            searchKeyword(keyword, pageNumber) // 검색한다면 다시 1페이지부터 요청
        }

        // 이전 페이지 버튼
        binding.ibPrevPage.setOnClickListener {
            closeFloatingButton() // 플로팅 버튼 닫기 & 현재 위치 추적 중지
            pageNumber--
            binding.tvPageNumber.text = pageNumber.toString()
            searchKeyword(keyword, pageNumber) // 이전 페이지 정보에 대해 요청
        }

        // 다음 페이지 버튼
        binding.ibNextPage.setOnClickListener {
            closeFloatingButton() // 플로팅 버튼 닫기 & 현재 위치 추적 중지
            pageNumber++
            binding.tvPageNumber.text = pageNumber.toString()
            searchKeyword(keyword, pageNumber) // 다음 페이지 정보에 대해 요청
        }

        // 현재 위치 추적 버튼
        binding.fabMyLocation.setOnClickListener{
            startTracking() // 위치 추적 시작
            toggleFab()     // 플로팅 액션 버튼
        }

        // 현재 위치 선택 버튼
        binding.fabSelectMyLocation.setOnClickListener {
            // 좌표를 바탕으로 현재 위치 불러오기 (Geocoder)
            address = Geocoder(applicationContext).getFromLocation(uLatitude, uLongitude, 10)
            try {
                if(address!=null){
                    if(address.isNullOrEmpty()) // 현재 위치 불러오기에 실패했다면
                        throw FailedGetLocationException("현재 위치를 불러오는데 실패하였습니다.")
                    else
                        Log.d("찾은 주소 : ", (address as MutableList<Address>)[0].toString())
                }
            }catch (e: FailedGetLocationException){
                e.printStackTrace()
                DialogUtil().SingleDialog(
                    applicationContext,
                    "현재 위치 정보를 불러오는데 문제가 발생하였습니다.",
                    "확인"
                )
            }
            // 사용자에게 현재 위치로 선택할 것인지 물어보기
            DialogUtil().MultipleDialog(
                this,
                "현재 위치는\n ${(address as MutableList<Address>)[0].getAddressLine(0).substring(5)}입니다.\n선택하시겠습니까?",
                "예",
                "아니오",
                {
                    // 현재 위치 정보 제보 페이지로 전달, 현재 위치의 경우 장소 이름과 도로명은 정보가 없으므로 ""로 전달.
                    val bundle = Bundle()
                    bundle.putSerializable("locationData", LocationList(
                        "", // 장소 이름
                        "", // 도로명
                        (address as MutableList<Address>)[0].getAddressLine(0).substring(5), // 전체 주소
                        uLongitude,
                        uLatitude
                    ))
                    intent.putExtras(bundle)
                    setResult(RESULT_OK, intent)
                    finish()
                },
                {
                    closeFloatingButton() // 위치 추적 중지하고 플로팅 액션 버튼 닫히도록
                    binding.fabSelectMyLocation.visibility = View.INVISIBLE // 액션 버튼 겹침 문제 해결
                }).show()
        }
    }

    /**
     * @description - 카카오 Map API 요청으로 키워드 검색을 진행하는 함수로, 요청 성공 시 검색 문자열과 매칭되는 장소들의 리스트를 응답으로 반환한다.
     * @param - keyword(String) : 위치 검색 문자열
     * @param - page(Int) : 현재 페이지 번호
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-08 | 2022-08-09
     */
    private fun searchKeyword(keyword: String, page: Int) {
        // Retrofit 서비스 객체 생성
        val service = RetrofitManager.retrofitKakao.create(RetrofitService::class.java)

        // API 서버에 요청
        service.getSearchKeyword(BuildConfig.kakaoRestApiKey, keyword, page).enqueue(object: Callback<ResponseLocationSearch> {
            override fun onResponse(call: Call<ResponseLocationSearch>, response: Response<ResponseLocationSearch>) {
                try {
                    if(response.isSuccessful)
                        addItemsAndMarkers(response.body())
                    else
                        throw ResponseErrorException("요청에 실패하였습니다. error: ${response.errorBody()?.string()}")
                }catch (e: ResponseErrorException){
                    e.printStackTrace()
                    DialogUtil().SingleDialog(
                        applicationContext,
                        "위치 검색에 문제가 발생하였습니다.",
                        "확인"
                    )
                }
            }

            override fun onFailure(call: Call<ResponseLocationSearch>, t: Throwable) {
                // 통신 실패
                Log.d(TAG, "통신 실패: ${t.message}")
            }
        })
    }

    /**
     * @description - 검색 결과 리스트에 대해 마커를 찍고, 리사이클러 뷰 어댑터에 추가하는 등의 처리를 하는 함수
     * @param - searchResult(ResponseLocationSearch?) : 키워드 검색 응답 DTO
     * @return - None
     * @author - Tae hyun Park
     * @since - 2022-08-08
     */
    private fun addItemsAndMarkers(searchResult: ResponseLocationSearch?) {
        if (!searchResult?.documents.isNullOrEmpty()) {
            // 검색 결과 있음
            listItems.clear()                   // 리스트 초기화
            binding.viewMap.removeAllPOIItems() // 지도의 마커 모두 제거
            for (document in searchResult!!.documents) {
                // 결과를 리사이클러 뷰에 추가
                val item = LocationList(document.placeName, // 장소 이름
                    document.roadAddressName,      // 도로명
                    document.addressName,     // 지번
                    document.x.toDouble(),     // 경도
                    document.y.toDouble())      // 위도
                listItems.add(item)

                // 지도에 마커 추가
                val marker = MapPOIItem()
                marker.apply { // marker 객체의 멤버 변수 할당
                    itemName = document.addressName // 마커를 눌렀을 때 보여줄 아이템 이름
                    mapPoint = MapPoint.mapPointWithGeoCoord(document.y.toDouble(), // 마커 위치(위도, 경도)
                        document.x.toDouble())
                    markerType = MapPOIItem.MarkerType.BluePin // 마커 색상
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin // 선택 시 마커 색상
                }
                binding.viewMap.addPOIItem(marker)
            }
            listAdapter.notifyDataSetChanged()

            binding.ibNextPage.isEnabled = !searchResult.meta.isEnd   // 페이지가 더 있을 경우 다음 버튼 활성화
            binding.ibPrevPage.isEnabled = pageNumber != 1            // 1페이지가 아닐 경우 이전 버튼 활성화
            binding.tvPageNumber.text = pageNumber.toString()

            // 이전, 다음 페이지 이미지 버튼
            if(!binding.ibPrevPage.isEnabled) // 사용 불가하면
                binding.ibPrevPage.setColorFilter(R.color.page_number_enable_false_color) // 비활성화 색상으로 변경
            else
                binding.ibPrevPage.setColorFilter(Color.BLACK) // 활성화 색상으로 변경

            if(!binding.ibNextPage.isEnabled) // 사용 불가하면
                binding.ibNextPage.setColorFilter(R.color.page_number_enable_false_color) // 비활성화 색상으로 변경
            else
                binding.ibNextPage.setColorFilter(Color.BLACK) // 활성화 색상으로 변경

        } else {
            // 검색 결과 없음
            Toast.makeText(this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * @description - 현재 자신의 위치를 추적 시작하는 함수로, 위도 경도 정보를 받아오고 해당 위치에 마커를 생성한다. 마커 생성 중복 방지를 위해 이미 마커가 있다면 지우고 생성한다.
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-09
     */
    @SuppressLint("MissingPermission") // 권한 재확인 안하기 위한 코드로, 코드 검사에서 제외할 부분을 미리 정의하는 것이라고 보면 됨.
    private fun startTracking() {
        val myLocationPOI = binding.viewMap.findPOIItemByTag(1)
        if(myLocationPOI != null) // 현재 위치를 표시하는 마커가 이미 있다면
            binding.viewMap.removePOIItem(myLocationPOI) // 마커 지우기

        binding.viewMap.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading  // 위치 추적 모드로 설정

        val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userNowLocation: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        // 위도 , 경도
        uLatitude = userNowLocation?.latitude!!
        uLongitude = userNowLocation?.longitude!!
        val uNowPosition = MapPoint.mapPointWithGeoCoord(uLatitude!!, uLongitude!!)

        // 현 위치에 마커 찍기
        val marker = MapPOIItem()
        marker.itemName = "현 위치"
        marker.mapPoint = uNowPosition
        marker.markerType = MapPOIItem.MarkerType.BluePin
        marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
        marker.tag = 1 // 마커 식별자
        binding.viewMap.addPOIItem(marker)
    }

    /**
     * @description - 위치 추적을 중지하는 함수
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-09
     */
    private fun stopTracking() {
        binding.viewMap.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
    }

    /**
     * @description - 플로팅 버튼 초기 상태로 초기화
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-10
     */
    private fun closeFloatingButton(){
        ObjectAnimator.ofFloat(binding.fabSelectMyLocation, "translationY", 0f).apply { start() } // 플로팅 액션 버튼 닫히기
        binding.fabMyLocation.setColorFilter(Color.BLACK) // 플로팅 버튼 색상 원상복귀
        stopTracking() // 위치 추적 기능 off
        isFabOpen = false
    }

    /**
     * @description - 플로팅 버튼의 애니메이션 함수
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-10
     */
    private fun toggleFab() {
        // 플로팅 액션 버튼 닫기
        if (isFabOpen){
            ObjectAnimator.ofFloat(binding.fabSelectMyLocation, "translationY", 0f).apply { start() }
            binding.fabMyLocation.setColorFilter(Color.BLACK)
            stopTracking() // 위치 추적도 중지
        }
        else{  // 플로팅 액션 버튼 열기
            ObjectAnimator.ofFloat(binding.fabSelectMyLocation, "translationY", -200f).apply { start() }
            binding.fabMyLocation.setColorFilter(Color.RED)
            binding.fabSelectMyLocation.visibility = View.VISIBLE // 액션 버튼 겹침 문제 해결
        }
        isFabOpen = !isFabOpen
    }

    /**
     * @description - 위치 관련 권한이 없는지 체크하고 없다면, 유저에게 요청하는 함수
     * @param - None
     * @return - None
     * @author - Tae Hyun Park
     * @since - 2022-08-09
     */
    private fun requestPermission(){
        // 권한 체크하고 없다면 권한 요청
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            var permissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }
}