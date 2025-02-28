import android.app.Application

class GlobalVariable : Application() {
    companion object {
        //存放變數
        private var Lightstate_1: Boolean = false
        private var Lightstate_2: Boolean = false
        private var Fanstate_1:Boolean = false
        private var AirConditioner_state:Boolean = false
        fun Change_Light_state_1(){
            this.Lightstate_1 = !Lightstate_1 //Change the value of the variable for Light_1
        }
        //取得電燈1的state
        fun Get_Light_state_1(): Boolean{    //Get the value of the variable for Light_1
            return Lightstate_1
        }
        fun Change_Light_state_2(){
            this.Lightstate_2 = !Lightstate_2 //Change the value of the variable for Light_2
        }
        //取得 變數值
        fun Get_Light_state_2(): Boolean{    //Get the value of the variable for Light_2
            return Lightstate_2
        }
        fun Change_Fan_state_1(){
            this.Fanstate_1 = !Fanstate_1 //Change the value of the variable for Fan_1
        }
        //取得 變數值
        fun Get_Fan_state_1(): Boolean{    //Get the value of the variable for Fan_1
            return Fanstate_1
        }
        fun Change_AirConditioner_state(){
            this.AirConditioner_state = !AirConditioner_state //Change the value of the variable for Fan_1
        }
        //取得 變數值
        fun Get_AirConditioner_state(): Boolean{    //Get the value of the variable for Fan_1
            return AirConditioner_state
        }
    }
}