plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val supplementaries_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")

    modImplementation("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}")

    modCompileOnly("curse.maven:yacl-667299:4574163")
    modCompileOnly("com.terraformersmc:modmenu:4.0.6")
    modCompileOnly("curse.maven:fabric-seasons-413523:4576886")
    modCompileOnly("curse.maven:farmers-delight-fabric-482834:4061213")
    modCompileOnly("curse.maven:entity-model-features-844662:8063559")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")

}