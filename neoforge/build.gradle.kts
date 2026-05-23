plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val supplementaries_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modCompileOnly("curse.maven:entity-model-features-844662:8063559")

    modCompileOnly("curse.maven:jei-238222:5846880")
    modCompileOnly("curse.maven:farmers-delight-398521:5772720")
    modCompileOnly("curse.maven:serene-seasons-291874:4577617")
    modCompileOnly("curse.maven:biomes-o-plenty-220318:4683058")
    modCompileOnly("curse.maven:terrablender-563928:4647094")
    modCompileOnly("curse.maven:configured-457570:5180900")

}