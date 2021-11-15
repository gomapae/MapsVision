package com.mapbox.vision.examples

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.vision.examples.model.PositionModel

/**
 * @description
 * @author shenfei.wang@g42.ai
 * @createtime 11/15/2021 10:53
 */
open class MainActivity: AppCompatActivity() {
//    W Hotel    25.106326435946634, 55.11137711295227
//    Warner Bros Dropoff 24.4911616087 54.5992251702
//    Warner Bros Pickup  24.493530180100883, 54.60146935306885
//    IKEA 24.48927458404 54.61327711436
//    Yas Mall 24.488975803225 54.60841084931
//    Yas Beach 24.462060347331082, 54.59223323992615
//    Yas Waterworld 24.48765603937617, 54.59968991294267

    var startLat: EditText? = null
    var startLng: EditText? = null
    var endLat: EditText? = null
    var endLng: EditText? = null

    var startEtFocued = true

    var list = mutableListOf<PositionModel>();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_enterance)
        initView()
        initData()
        initEvent()
    }

    private fun initView() {
        startLat = findViewById(R.id.et_start_lat)
        startLng = findViewById(R.id.et_start_lng)
        endLat = findViewById(R.id.et_end_lat)
        endLng = findViewById(R.id.et_end_lng)
    }

    private fun initData() {
        list.add(PositionModel(55.11137711295227, 25.106326435946634))
        list.add(PositionModel(55.5992251702, 25.4911616087))
        list.add(PositionModel(54.60146935306885, 24.493530180100883))
        list.add(PositionModel(55.61327711436, 25.48927458404))
        list.add(PositionModel(55.60841084931, 25.488975803225))
        list.add(PositionModel(55.59223323992615, 25.462060347331082))
        list.add(PositionModel(55.59968991294267, 25.48765603937617))
    }

    private fun initEvent () {
        startLat?.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                startEtFocued = true
            }
        }
        startLng?.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                startEtFocued = true
            }
        }
        endLat?.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                startEtFocued = false
            }
        }
        endLng?.onFocusChangeListener = View.OnFocusChangeListener { view, b ->
            if (b) {
                startEtFocued = false
            }
        }
    }

    fun startNavigation(v: View?) {
        if (startLat?.text.isNullOrEmpty() || startLng?.text.isNullOrEmpty()
                || endLat?.text.isNullOrEmpty() || endLng?.text.isNullOrEmpty()) {
                    Toast.makeText(this, "Please input or select flowing site", Toast.LENGTH_SHORT).show()
            return
        }
        var intent = Intent(this, ArActivityKt::class.java)
        intent.putExtra("startLat", startLat?.text.toString())
        intent.putExtra("startLng", startLng?.text.toString())
        intent.putExtra("endLat", endLat?.text.toString())
        intent.putExtra("endLng", endLng?.text.toString())
        startActivity(intent)
    }

    fun itemSelected(v: View?) {
        if (v?.id == R.id.l_site_0) {
            if (startEtFocued) {
                inputStartLatLng(list[0])
            } else {
                inputEndLatLng(list[0])
            }
        } else if (v?.id == R.id.l_site_1) {
            if (startEtFocued) {
                inputStartLatLng(list[1])
            } else {
                inputEndLatLng(list[1])
            }
        } else if (v?.id == R.id.l_site_2) {
            if (startEtFocued) {
                inputStartLatLng(list[2])
            } else {
                inputEndLatLng(list[2])
            }
        } else if (v?.id == R.id.l_site_3) {
            if (startEtFocued) {
                inputStartLatLng(list[3])
            } else {
                inputEndLatLng(list[3])
            }
        } else if (v?.id == R.id.l_site_4) {
            if (startEtFocued) {
                inputStartLatLng(list[4])
            } else {
                inputEndLatLng(list[4])
            }
        } else if (v?.id == R.id.l_site_5) {
            if (startEtFocued) {
                inputStartLatLng(list[5])
            } else {
                inputEndLatLng(list[5])
            }
        } else if (v?.id == R.id.l_site_6) {
            if (startEtFocued) {
                inputStartLatLng(list[6])
            } else {
                inputEndLatLng(list[6])
            }
        }
    }

    private fun inputStartLatLng(model: PositionModel) {
        startLat?.setText(model.lat.toString())
        startLng?.setText(model.lng.toString())
        endLat?.requestFocus()
    }
    private fun inputEndLatLng(model: PositionModel) {
        endLat?.setText(model.lat.toString())
        endLng?.setText(model.lng.toString())
    }
}