package fr.miage.kadi.catalogue.adapter.out.persistence;

final class NormalisationCatalogueSql {
    static final String EXPRESSION = """
            lower(translate(%s,
                'ÀÂÄÁÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸàâäáãåçéèêëíìîïñóòôöõúùûüýÿ',
                'AAAAAACEEEEIIIINOOOOOUUUUYYaaaaaaceeeeiiiinooooouuuuyy'))
            """;

    private NormalisationCatalogueSql() {
    }
}
