package ru.nucodelabs.gem.app.io.anisotropy

import com.fasterxml.jackson.databind.ObjectMapper
import ru.nucodelabs.gem.app.io.project.AbstractProjectFileServiceImpl
import ru.nucodelabs.gem.app.io.project.Project
import ru.nucodelabs.gem.file.dto.anisotropy.PointDto
import ru.nucodelabs.gem.file.dto.project.ProjectDto
import ru.nucodelabs.gem.file.dto.project.ProjectDtoMapper
import ru.nucodelabs.geo.anisotropy.Point
import javax.inject.Inject

class PointProjectFileServiceImpl @Inject constructor(
    objectMapper: ObjectMapper,
    private val dtoMapper: ProjectDtoMapper
) : AbstractProjectFileServiceImpl<Point>(objectMapper) {
    @Suppress("UNCHECKED_CAST")
    override fun mapFromDto(projectDto: ProjectDto<*>): Project<Point> {
        return dtoMapper.toPointProject(projectDto as ProjectDto<PointDto>)
    }

    override fun mapToDto(project: Project<Point>): ProjectDto<*> {
        return dtoMapper.fromPointProject(project)
    }
}