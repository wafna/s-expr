package wafna.sexpr

import kotlin.test.Test

internal sealed interface Level0
internal data class Data0(val x: String) : Level0
internal sealed interface Level1 : Level0
internal data class Data1(val y: Int) : Level1
internal data class Data2(val z: Double) : Level1

internal data class Data123(val data: List<Level0>)

class TestMapperSealed {
    @Test
    fun `multi-level sealed data class hierarchy`() {
        Mappers {
            adapt<Level0>()
            adapt<Data123>()
        }.apply {
//            testObject(Data0("data-0"))
//            testObject(Data1(99))
//            testObject(Data2(-0.444))
            testObject(
                Data123(
                    listOf(Data0("data-0-0"), Data1(99), Data2(6.6666))
                )
            )
        }
    }
}