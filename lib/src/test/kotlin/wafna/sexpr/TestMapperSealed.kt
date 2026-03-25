package wafna.sexpr

import kotlin.test.Test

internal sealed interface Level0
internal data class Data0(val x: String) : Level0
internal sealed interface Level1 : Level0
internal data class Data1(val y: String) : Level1
internal data class Data2(val z: String) : Level1

internal data class Data123(val data0: List<Data0>, val data1: List<Data1>, val data2: List<Data2>)

class TestMapperSealed {
    @Test
    fun `multi-level sealed data class hierarchy`() {
        Mappers {
            register<Level0>()
            register<Data123>()
        }.apply {
            testObject(Data0("data-0"))
            testObject(Data1("data-1"))
            testObject(Data2("data-2"))
            testObject(
                Data123(
                    listOf(Data0("data-0-0"), Data0("data-0-1")),
                    listOf(Data1("data-1-0"), Data1("data-1-1")),
                    listOf(Data2("data-2-0"), Data2("data-2-1"))
                )
            )
        }
    }
}