package ru.netology.nmedia.repository

import androidx.lifecycle.*
import okio.IOException
import retrofit2.Response
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    // функция getAll() возвращает подписку на посты LiveData<List<PostEntity>>
    override val data =
        dao.getAll().map(List<PostEntity>::toDto) // преобразуем List<PostEntity> в List<Post>

    override suspend fun getAll() {
        try {
            // делаем запрос на получение списка постов с сервера
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается List<Post>
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // заменяем список постов в базе данных на тот, что получили с сервера
            dao.insert(body.toEntity()) // body.toEntity() - преобразуем List<Post> в List<PostEntity>
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            // делаем запрос на сохранение поста на сервере
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается Post
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // сохраняем пост в базе данных
            dao.insert(PostEntity.fromDto(body)) // PostEntity.fromDto(body) - преобразуем Post в PostEntity
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            // удаляем пост в базе данных
            dao.removeById(id)

            // делаем запрос на удаление поста на сервере
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                throw ApiError(response.code(), response.message())
            }


        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        var postFindByIdOld = dao.findById(id)
        try {
            // сохраняем пост в базе данных
            val postFindByIdNew = postFindByIdOld.copy(
                likedByMe = !postFindByIdOld.likedByMe,
                likes = postFindByIdOld.likes + if (postFindByIdOld.likedByMe) -1 else 1
            )
            dao.insert(postFindByIdNew)

            // делаем запрос на изменение лайка поста на сервере
            val response: Response<Post> = if (!postFindByIdOld.likedByMe) {
                PostsApi.service.likeById(id)
            } else {
                PostsApi.service.dislikeById(id)
            }
            if (!response.isSuccessful) { // если запрос прошёл неуспешно, выбросить исключение
                dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
                throw ApiError(response.code(), response.message())
            }

            // в качетве тела запроса нам возвращается Post
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            // сохраняем пост в базе данных
            dao.insert(PostEntity.fromDto(body)) // PostEntity.fromDto(body) - преобразуем Post в PostEntity
        } catch (e: IOException) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(postFindByIdOld) // вернём базу данных к исходному виду
            throw UnknownError
        }
    }

}
