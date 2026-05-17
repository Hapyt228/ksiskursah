package by.bsuir.filmcatalog.config;

import by.bsuir.filmcatalog.model.Film;
import by.bsuir.filmcatalog.repository.FilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Инициализатор тестовых данных.
 * Заполняет базу данных при первом запуске приложения.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final FilmRepository filmRepository;

    @Autowired
    public DataInitializer(FilmRepository filmRepository) {
        this.filmRepository = filmRepository;
    }

    @Override
    public void run(String... args) {
        // Заполняем только если БД пустая
        if (filmRepository.count() > 0) {
            return;
        }

        filmRepository.save(new Film(
            "Побег из Шоушенка", "The Shawshank Redemption", 1994,
            "Драма", "Фрэнк Дарабонт", "США",
            "Банкир Энди Дюфрейн обвинён в убийстве собственной жены и её любовника. Оказавшись в тюрьме под названием Шоушенк, он сталкивается с жестокой реальностью — коррумпированным надзирателем, жестокими заключёнными и потерей свободы. Несмотря на это, Энди не теряет надежды и борется за своё достоинство.",
            9.3, 142,
            "https://m.media-amazon.com/images/M/MV5BNDE3ODcxYzMtY2YzZC00NmNlLWJiNDMtZDViZWM2MzIxZDYwXkEyXkFqcGdeQXVyNjAwNDUxODI@._V1_SX300.jpg",
            "Английский", "тюрьма,дружба,надежда,классика"
        ));

        filmRepository.save(new Film(
            "Крёстный отец", "The Godfather", 1972,
            "Криминал", "Фрэнсис Форд Коппола", "США",
            "Дон Вито Корлеоне — глава мафиозного клана. Когда его отвергает другой босс, начинается война между семьями. Его младший сын Майкл, не желавший участвовать в семейном бизнесе, вынужден взять на себя управление семьёй.",
            9.2, 175,
            "https://m.media-amazon.com/images/M/MV5BM2MyNjYxNmUtYTAwNi00MTYxLWJmNWYtYzZlODY3ZTk3OTFlXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
            "Английский", "мафия,семья,власть,классика"
        ));

        filmRepository.save(new Film(
            "Тёмный рыцарь", "The Dark Knight", 2008,
            "Боевик", "Кристофер Нолан", "США",
            "Бэтмен вступает в противостояние с Джокером — преступным гением, который хочет погрузить Готэм в хаос. Фильм исследует вопросы морали, жертвенности и природы зла.",
            9.0, 152,
            "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_SX300.jpg",
            "Английский", "супергерой,Джокер,Бэтмен,экшн"
        ));

        filmRepository.save(new Film(
            "Список Шиндлера", "Schindler's List", 1993,
            "Драма", "Стивен Спилберг", "США",
            "История немецкого предпринимателя Оскара Шиндлера, который во время Второй мировой войны спас тысячи польских евреев от нацистских лагерей смерти.",
            9.0, 195,
            "https://m.media-amazon.com/images/M/MV5BNJE1MDkzNTAtNGYxMy00YWM2LWI3NTItM2NhMDgyNmVkYWIwXkEyXkFqcGdeQXVyMjUzOTY1NTc@._V1_SX300.jpg",
            "Английский", "война,Холокост,история,черно-белый"
        ));

        filmRepository.save(new Film(
            "Интерстеллар", "Interstellar", 2014,
            "Фантастика", "Кристофер Нолан", "США",
            "В недалёком будущем Земля умирает. Группа астронавтов отправляется через червоточину в поисках новой пригодной для жизни планеты. Фильм исследует темы любви, времени и пространства.",
            8.6, 169,
            "https://m.media-amazon.com/images/M/MV5BZjdkOTU3MDktN2IxOS00OGEyLWFmMjktY2FiMmZkNWIyODZiXkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_SX300.jpg",
            "Английский", "космос,время,физика,путешествия"
        ));

        filmRepository.save(new Film(
            "Начало", "Inception", 2010,
            "Фантастика", "Кристофер Нолан", "США",
            "Дом Кобб — вор, специализирующийся на краже идей прямо из снов людей. Ему предлагают последнее задание: внедрить идею в разум человека. Это называется «начало».",
            8.8, 148,
            "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_SX300.jpg",
            "Английский", "сны,реальность,воровство,разум"
        ));

        filmRepository.save(new Film(
            "Форрест Гамп", "Forrest Gump", 1994,
            "Драма", "Роберт Земекис", "США",
            "Жизнь простого человека с низким IQ, который невольно оказывается свидетелем и участником ключевых событий американской истории второй половины XX века.",
            8.8, 142,
            "https://m.media-amazon.com/images/M/MV5BNWIwODRlZTUtY2U3ZS00Yzg1LWJhNzYtMmZiYmEyNmU1NjMzXkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_SX300.jpg",
            "Английский", "история,жизнь,любовь,американская мечта"
        ));

        filmRepository.save(new Film(
            "Властелин колец: Возвращение короля", "The Lord of the Rings: The Return of the King", 2003,
            "Фэнтези", "Питер Джексон", "Новая Зеландия",
            "Финальная часть эпической трилогии. Фродо и Сэм приближаются к Мордору, чтобы уничтожить Кольцо. Арагорн собирает все силы Средиземья для финальной битвы.",
            8.9, 201,
            "https://m.media-amazon.com/images/M/MV5BNzA5ZDJhZWMtOWU5YS00ZjZhLThmNmItYzU3MzQwZjQ3OWQxXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
            "Английский", "кольцо,хоббит,эпос,средиземье"
        ));

        filmRepository.save(new Film(
            "Матрица", "The Matrix", 1999,
            "Фантастика", "Сёстры Вачовски", "США",
            "Программист Нео обнаруживает, что реальность, в которой он живёт — это компьютерная симуляция. Он присоединяется к группе повстанцев, борющихся против машин.",
            8.7, 136,
            "https://m.media-amazon.com/images/M/MV5BNzQzOTk3OTAtNDQ0Zi00ZTVlLTM5YTUtZGZmYWMwYjc4OTQzXkEyXkFqcGdeQXVyNjU0OTQ0OTY@._V1_SX300.jpg",
            "Английский", "симуляция,реальность,боевик,философия"
        ));

        filmRepository.save(new Film(
            "Хороший, плохой, злой", "The Good, the Bad and the Ugly", 1966,
            "Вестерн", "Серджо Леоне", "Италия",
            "Три авантюриста ищут золото армии Конфедерации, спрятанное на кладбище. Каждый из них знает только часть информации, а потому они вынуждены сотрудничать, оставаясь врагами.",
            8.8, 178,
            "https://m.media-amazon.com/images/M/MV5BNjJlYmNkZGItM2NhYy00MjlmLTk5NmQtNjg1NmM2ODU4OTMwXkEyXkFqcGdeQXVyMjUzOTY1NTc@._V1_SX300.jpg",
            "Итальянский", "вестерн,золото,война,Клинт Иствуд"
        ));

        filmRepository.save(new Film(
            "Бойцовский клуб", "Fight Club", 1999,
            "Триллер", "Дэвид Финчер", "США",
            "Офисный работник, страдающий бессонницей, встречает загадочного торговца мылом Тайлера Дёрдена. Вместе они основывают подпольный бойцовский клуб, который перерастает в нечто большее.",
            8.8, 139,
            "https://m.media-amazon.com/images/M/MV5BMmEzNTkxYjQtZTc0MC00YTVjLTg5ZTEtZWMwOWVlYzY0NWIwXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
            "Английский", "психология,анархия,идентичность,культ"
        ));

        filmRepository.save(new Film(
            "Криминальное чтиво", "Pulp Fiction", 1994,
            "Криминал", "Квентин Тарантино", "США",
            "Переплетённые истории преступников, боксёра и пары грабителей в Лос-Анджелесе. Фильм нелинейно рассказывает о насилии, искуплении и случайностях.",
            8.9, 154,
            "https://m.media-amazon.com/images/M/MV5BNGNhMDIzZTUtNTBlZi00MTRlLWFjM2ItYzViMjE3YzI5MjljXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
            "Английский", "Тарантино,нелинейность,преступность,культ"
        ));

        filmRepository.save(new Film(
            "Зелёная миля", "The Green Mile", 1999,
            "Драма", "Фрэнк Дарабонт", "США",
            "Тюремный надзиратель Пол Эджкомб рассказывает о своей службе в блоке смертников и встрече с приговорённым Джоном Коффи, обладающим сверхъестественным даром.",
            8.6, 189,
            "https://m.media-amazon.com/images/M/MV5BMTUxMzQyNjA5MF5BMl5BanBnXkFtZTYwOTU2NTY3._V1_SX300.jpg",
            "Английский", "тюрьма,чудо,смерть,дружба"
        ));

        filmRepository.save(new Film(
            "Брат", null, 1997,
            "Криминал", "Алексей Балабанов", "Россия",
            "Молодой человек Данила Багров приезжает в Санкт-Петербург к своему брату и постепенно втягивается в криминальный мир города. Культовый российский фильм 90-х.",
            7.8, 99,
            "https://m.media-amazon.com/images/M/MV5BOTRmNzI5NzMtY2MxYy00ZGQ2LTlkMTktZmM0MmZiOWU4NzI4XkEyXkFqcGdeQXVyNzgyMDkwMDU@._V1_SX300.jpg",
            "Русский", "Санкт-Петербург,90-е,криминал,культ"
        ));

        filmRepository.save(new Film(
            "Сталкер", null, 1979,
            "Фантастика", "Андрей Тарковский", "СССР",
            "Проводник ведёт двух людей — Писателя и Профессора — в загадочную Зону, в центре которой находится Комната, где исполняются заветные желания.",
            8.1, 162,
            "https://m.media-amazon.com/images/M/MV5BMDgwODNmM2ItMGE3Mi00NjZkLTk1MTItYTlmYWY1YjI5YmMzXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
            "Русский", "Тарковский,СССР,философия,зона"
        ));

        System.out.println("✓ База данных заполнена тестовыми данными: " + filmRepository.count() + " фильмов");
    }
}
