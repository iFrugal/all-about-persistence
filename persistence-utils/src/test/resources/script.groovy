print('loading script.js....');

def f1(){
    print("F11111111");
}

def f2(){
    return "F2";
}

def f3(a){
    return "F3" + "-" + a;
}

def f4(obj){
    def obj1 = [:];
    obj1['x'] = obj['a'] + "_x";
    obj1['y'] = obj['b'] + "_y";
    return obj1;
}

def printA(){
    print(a1);
}