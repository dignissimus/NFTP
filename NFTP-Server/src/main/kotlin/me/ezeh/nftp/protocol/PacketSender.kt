package me.ezeh.nftp.protocol


abstract class PacketSender {
    protected fun concatIntArrays(vararg arrays: IntArray): IntArray {
        var finished = IntArray(0)
        for (array in arrays) {
            val temp = IntArray(finished.size + array.size)
            //System.out.println("t: " + temp.length + " f: " + finished.length + " a: " + a.length);
            System.arraycopy(finished, 0, temp, 0, finished.size)
            System.arraycopy(array, 0, temp, finished.size, array.size)
            finished = temp
        }
        return finished
    }

    protected fun createStringPacket(name: String): IntArray {
        val bytearray = name.toByteArray()
        val intarray = IntArray(bytearray.size)
        for (index in bytearray.indices) {
            intarray[index] = bytearray[index].toInt()
        }
        val lengthPacket = intArrayOf(Bytes.LENGTH, intarray.size)
        return concatIntArrays(lengthPacket, intarray)
    }
}
