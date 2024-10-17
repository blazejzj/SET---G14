package DatabaseTests;

import database.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

public class DatabaseSaveDataTests {

    private Database database;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;

    private ResultSet createMockResultSetForGeneratedKeys(int generatedId) throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(generatedId);
        return mockResultSet;
    }

    @BeforeEach
    public void setUp() throws SQLException {

        database = spy(new Database("test.db")); // create a spy

        // Mock connection and prepared statementss
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);

        // Set up the mock to return mockStatement when any SQL string and any integer  (which is normally used for specifying keys that are autogenerated)
        // are passed to further to prepareStatement.
        // This basically allows us to simulate preparing a SQL insert query and returning a mock PreparedStatement
        // which then allows us to verify if the correct SQL command is executed and with the right params
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);

        // Override connect method in database
        // prevent the actual db from opening -> instead use mock connection
        // that allows ut o test logic and stuff without affecting real db
        doReturn(mockConnection).when(database).connect();
    }

    @Test
    @DisplayName("Save an user / Insert a new row into User table")
    public void testSaveUserIntoDatabase() throws SQLException {
        ResultSet mockResultSet = createMockResultSetForGeneratedKeys(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        database.saveUser("Blazej");

        verify(mockConnection).prepareStatement(
                "INSERT INTO Users (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS
        );
        verify(mockPreparedStatement).setString(1, "Blazej");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Saved user returns newly generated ID")
    public void testSaveUserReturnsNewlyGeneratedId() throws SQLException {
        ResultSet mockResultSet1 = createMockResultSetForGeneratedKeys(1);
        ResultSet mockResultSet2 = createMockResultSetForGeneratedKeys(2);

        when(mockPreparedStatement.getGeneratedKeys())
                .thenReturn(mockResultSet1)
                .thenReturn(mockResultSet2);

        int userID1 = database.saveUser("Blazej");
        int userID2 = database.saveUser("Kamilla");

        assertNotEquals(userID1, userID2);
    }

    @Test
    @DisplayName("Save a project into the database")
    public void testSaveProject() throws  SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        // id automatically generates, hence we make a project assigning an id
        // 1arg = title, 2arg = description, 3arg = userid holding the project
        int projectId = database.saveProject("ProjectName", "ProjectDescription", 1);

        verify(mockPreparedStatement).setString(1, "ProjectName");
        verify(mockPreparedStatement).setString(2, "ProjectDescription");
        verify(mockPreparedStatement).setInt(3, 1);
        verify(mockPreparedStatement).executeUpdate();

        assertEquals(1, projectId);
    }
}
